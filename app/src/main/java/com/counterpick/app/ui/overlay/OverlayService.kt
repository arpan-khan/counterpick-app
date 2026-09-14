package com.counterpick.app.ui.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import coil.target.Target
import com.counterpick.app.CounterPickApp
import com.counterpick.app.MainActivity
import com.counterpick.app.R
import com.counterpick.app.data.local.entity.HeroEntity
import com.counterpick.app.scoring.CounterResult
import com.counterpick.app.scoring.DraftScorer
import com.counterpick.app.scoring.FilterEngine
import com.counterpick.app.scoring.FromYourListsEngine
import com.counterpick.app.scoring.FromYourListsResult
import com.counterpick.app.scoring.MainRecord
import com.counterpick.app.scoring.RoleLaneFilterState
import com.counterpick.app.scoring.RoleLaneTagged
import com.counterpick.app.scoring.RoleLaneUniverse
import com.counterpick.app.scoring.buildByMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var heroesById: Map<Int, HeroEntity> = emptyMap()
    private var byMain: Map<Int, MainRecord> = emptyMap()
    private var heroesLoaded = false

    private val enemySlotIds = arrayOfNulls<Int>(MAX_SLOTS)
    private var activePickerSlot: Int? = null
    private var filterState = RoleLaneFilterState()

    private var lastX = 100
    private var lastY = 300

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
        loadData()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (bubbleView == null && panelView == null) showBubble()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeBubble()
        removePanel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun loadData() {
        val app = application as CounterPickApp
        serviceScope.launch {
            val heroes = app.repository.getAllHeroes()
            heroesById = heroes.associateBy { it.id }
            byMain = buildByMain(app.repository.getAllMatchups())
            heroesLoaded = true
        }
    }

    private fun showBubble() {
        removePanel()
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)

        val params = overlayLayoutParams(BUBBLE_SIZE_DP.dp(), BUBBLE_SIZE_DP.dp())
        params.gravity = Gravity.TOP or Gravity.START
        params.x = lastX
        params.y = lastY

        attachDragAndClick(
            view = view,
            params = params,
            onTap = { showPanel() },
            onMoved = { x, y -> lastX = x; lastY = y }
        )

        windowManager.addView(view, params)
        bubbleView = view
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun showPanel() {
        removeBubble()
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_panel, null)

        val params = overlayLayoutParams(PANEL_WIDTH_DP.dp(), WindowManager.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.TOP or Gravity.START
        params.x = lastX
        params.y = lastY
        panelParams = params

        wirePanel(view)

        val header = view.findViewById<View>(R.id.panel_collapse).parent as View
        attachDragAndClick(
            view = header,
            params = params,
            onTap = {  },
            onMoved = { x, y -> lastX = x; lastY = y },
            targetViewForWindowUpdate = view
        )

        windowManager.addView(view, params)
        panelView = view
        renderEnemySlots(view)
        renderFilterChips(view)
        renderEmptyResults(view)
    }

    private fun removePanel() {
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
        panelParams = null
    }

    private fun wirePanel(view: View) {
        view.findViewById<View>(R.id.panel_collapse).setOnClickListener { showBubble() }
        view.findViewById<View>(R.id.panel_close).setOnClickListener { stopSelf() }
        view.findViewById<View>(R.id.find_best_picks_button).setOnClickListener { runDraftAssistant(view) }

        val searchInput = view.findViewById<EditText>(R.id.picker_search)
        searchInput.setOnFocusChangeListener { _, hasFocus -> setPanelFocusable(hasFocus) }
        searchInput.addTextChangedListener(simpleWatcher { query -> populatePickerList(view, query) })
    }

    private fun setPanelFocusable(focusable: Boolean) {
        val params = panelParams ?: return
        val panel = panelView ?: return
        params.flags = if (focusable) {
            params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { windowManager.updateViewLayout(panel, params) }
    }

    private fun buildAvatar(sizeDp: Int, heroId: Int, heroName: String, imageUrl: String?): FrameLayout {
        val sizePx = sizeDp.dp()
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx)
        }
        val initials = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = coloredCircle(colorForHero(heroId))
            gravity = Gravity.CENTER
            textSize = (sizeDp * 0.4f)
            setTextColor(Color.WHITE)
            text = heroName.take(1).uppercase()
        }
        frame.addView(initials)

        if (!imageUrl.isNullOrBlank()) {
            val image = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = coloredCircle(Color.TRANSPARENT)
                clipToOutline = true
                visibility = View.GONE
            }
            frame.addView(image)
            val request = ImageRequest.Builder(this)
                .data(imageUrl)
                .target(object : Target {
                    override fun onSuccess(result: Drawable) {
                        image.setImageDrawable(result)
                        image.visibility = View.VISIBLE
                    }
                    override fun onError(error: Drawable?) {  }
                })
                .build()
            runCatching { Coil.imageLoader(this).enqueue(request) }
        }
        return frame
    }

    private fun buildAvatar(sizeDp: Int, hero: HeroEntity?): FrameLayout =
        buildAvatar(sizeDp, hero?.id ?: 0, hero?.name ?: "?", hero?.image)

    private fun renderFilterChips(view: View) {
        val roleRow = view.findViewById<LinearLayout>(R.id.role_filter_row)
        roleRow.removeAllViews()
        roleRow.addView(buildChip("All Roles", filterState.roles == RoleLaneUniverse.ROLES.toSet()) {
            filterState = filterState.withAllRoles(true)
            renderFilterChips(view)
            runDraftAssistant(view)
        })
        RoleLaneUniverse.ROLES.forEach { role ->
            roleRow.addView(buildChip(role, filterState.roles == setOf(role)) {
                filterState = if (filterState.roles == setOf(role)) {
                    filterState.withAllRoles(true)
                } else {
                    filterState.copy(roles = setOf(role))
                }
                renderFilterChips(view)
                runDraftAssistant(view)
            })
        }

        val laneRow = view.findViewById<LinearLayout>(R.id.lane_filter_row)
        laneRow.removeAllViews()
        laneRow.addView(buildChip("All Lanes", filterState.lanes == RoleLaneUniverse.LANES.toSet()) {
            filterState = filterState.withAllLanes(true)
            renderFilterChips(view)
            runDraftAssistant(view)
        })
        RoleLaneUniverse.LANES.forEach { lane ->
            laneRow.addView(buildChip(lane, filterState.lanes == setOf(lane)) {
                filterState = if (filterState.lanes == setOf(lane)) {
                    filterState.withAllLanes(true)
                } else {
                    filterState.copy(lanes = setOf(lane))
                }
                renderFilterChips(view)
                runDraftAssistant(view)
            })
        }
    }

    private fun buildChip(label: String, selected: Boolean, onClick: () -> Unit): View =
        TextView(this).apply {
            text = label
            textSize = 10.5f
            setPadding(12.dp(), 5.dp(), 12.dp(), 5.dp())
            setTextColor(Color.parseColor(if (selected) "#191308" else "#EEF1F6"))
            setBackgroundResource(if (selected) R.drawable.overlay_chip_selected else R.drawable.overlay_chip_unselected)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = 6.dp() }
            setOnClickListener { onClick() }
        }

    private fun renderEnemySlots(view: View) {
        val row = view.findViewById<LinearLayout>(R.id.enemy_slot_row)
        row.removeAllViews()
        for (slot in 0 until MAX_SLOTS) {
            val heroId = enemySlotIds[slot]
            val hero = heroId?.let { heroesById[it] }
            val slotView: View = if (hero != null) {
                buildAvatar(40, hero).apply {
                    (layoutParams as LinearLayout.LayoutParams).marginEnd = 6.dp()
                    setOnClickListener { enemySlotIds[slot] = null; renderEnemySlots(view) }
                }
            } else {
                TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(40.dp(), 40.dp()).also { it.marginEnd = 6.dp() }
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(Color.parseColor("#EEF1F6"))
                    setBackgroundResource(R.drawable.overlay_slot_bg)
                    text = "+"
                    setOnClickListener { openPickerForSlot(view, slot) }
                }
            }
            row.addView(slotView)
        }
    }

    private fun openPickerForSlot(view: View, slot: Int) {
        activePickerSlot = slot
        view.findViewById<ScrollView>(R.id.results_scroll).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.picker_container).visibility = View.VISIBLE
        view.findViewById<EditText>(R.id.picker_search).apply {
            setText("")
            requestFocus()
        }
        populatePickerList(view, "")
    }

    private fun closePicker(view: View) {
        activePickerSlot = null
        view.findViewById<LinearLayout>(R.id.picker_container).visibility = View.GONE
        view.findViewById<ScrollView>(R.id.results_scroll).visibility = View.VISIBLE
        setPanelFocusable(false)
    }

    private fun populatePickerList(view: View, query: String) {
        val listView = view.findViewById<ListView>(R.id.picker_list)
        val matches = heroesById.values
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.name }
            .take(30)

        listView.adapter = HeroRowAdapter(this, matches)
        listView.setOnItemClickListener { _, _, position, _ ->
            val hero = matches[position]
            val slot = activePickerSlot
            if (slot != null) {
                enemySlotIds[slot] = hero.id
                renderEnemySlots(view)
            }
            closePicker(view)
        }
    }

    private fun runDraftAssistant(view: View) {
        if (!heroesLoaded) return
        val enemyIds = enemySlotIds.filterNotNull()
        val container = view.findViewById<LinearLayout>(R.id.results_container)
        val emptyText = view.findViewById<TextView>(R.id.empty_state_text)
        container.removeAllViews()

        if (enemyIds.isEmpty()) {
            emptyText.text = "Add at least one enemy hero."
            emptyText.visibility = View.VISIBLE
            renderFromYourLists(view, emptyList())
            return
        }

        val ranked = DraftScorer.rank(enemyIds, byMain)
        val pool = DraftScorer.topN(ranked, n = 15)
        val filtered = pool.filter { result ->
            val hero = heroesById[result.heroId]
            val tagged = object : RoleLaneTagged {
                override val roles = hero?.roles.orEmpty()
                override val lanes = hero?.lanes.orEmpty()
            }
            FilterEngine.matches(tagged, filterState)
        }
        val top = filtered.take(8)

        if (top.isEmpty()) {
            emptyText.text = "No matches for these heroes with the current filters."
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
            top.forEachIndexed { index, result -> container.addView(buildResultRow(index + 1, result)) }
        }

        renderFromYourLists(view, enemyIds)
    }

    private fun renderFromYourLists(view: View, enemyIds: List<Int>) {
        val header = view.findViewById<TextView>(R.id.from_lists_header)
        val container = view.findViewById<LinearLayout>(R.id.from_lists_container)
        container.removeAllViews()

        if (enemyIds.isEmpty()) {
            header.visibility = View.GONE
            return
        }

        serviceScope.launch {
            val app = application as CounterPickApp
            val activeList = app.customListsRepository.getActiveList()
            val rows = if (activeList != null) app.customListsRepository.getCounterRowsForList(activeList.listId) else emptyList()
            val results = FromYourListsEngine.compute(enemyIds, rows)

            if (results.isEmpty()) {
                header.visibility = View.GONE
                return@launch
            }
            header.visibility = View.VISIBLE
            container.removeAllViews()
            results.forEach { r -> container.addView(buildFromYourListsRow(r)) }
        }
    }

    private fun buildFromYourListsRow(result: FromYourListsResult): View {
        val hero = heroesById[result.heroId]
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.overlay_row_bg)
            setPadding(8.dp(), 6.dp(), 8.dp(), 6.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 4.dp() }
        }
        row.addView(buildAvatar(24, hero).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = 8.dp()
        })
        row.addView(TextView(this).apply {
            text = hero?.name ?: "Unknown"
            setTextColor(Color.parseColor("#EEF1F6"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "counters ${result.matchedEnemyCount} · your #${result.bestPosition + 1}"
            setTextColor(Color.parseColor("#8891A3"))
            textSize = 10.5f
        })
        return row
    }

    private fun buildResultRow(rank: Int, result: CounterResult): View {
        val hero = heroesById[result.heroId]
        val isGood = result.total >= 0

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.overlay_row_bg)
            setPadding(8.dp(), 6.dp(), 8.dp(), 6.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 4.dp() }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = rank.toString()
            setTextColor(Color.parseColor("#8891A3"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(18.dp(), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        header.addView(buildAvatar(26, hero).apply {
            (layoutParams as LinearLayout.LayoutParams).marginStart = 2.dp()
        })
        header.addView(TextView(this).apply {
            text = hero?.name ?: "Unknown"
            setTextColor(Color.parseColor("#EEF1F6"))
            textSize = 12.5f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = 8.dp()
            }
        })
        header.addView(TextView(this).apply {
            text = "${if (isGood) "+" else ""}${"%.2f".format(result.total * 100)}%"
            setTextColor(Color.parseColor(if (isGood) "#2FD883" else "#FF4D5E"))
            textSize = 12f
        })
        container.addView(header)

        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 4.dp() }
        }
        result.matched.forEach { m ->
            val enemyName = heroesById[m.enemyId]?.name ?: "Unknown"
            val line = if (m.type == com.counterpick.app.scoring.MatchType.GOOD) {
                "Beats $enemyName · #${m.rank} · +${"%.2f".format(m.score * 100)}%"
            } else {
                "$enemyName beats this hero · #${m.rank} · ${"%.2f".format(m.score * 100)}%"
            }
            details.addView(TextView(this).apply {
                text = line
                textSize = 10.5f
                setTextColor(Color.parseColor("#8891A3"))
                setPadding(0, 2.dp(), 0, 2.dp())
            })
        }
        container.addView(details)

        header.setOnClickListener {
            details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        return container
    }

    private fun renderEmptyResults(view: View) {
        view.findViewById<TextView>(R.id.empty_state_text).apply {
            text = "Add enemies above, then tap Find best picks."
            visibility = View.VISIBLE
        }
    }

    private fun attachDragAndClick(
        view: View,
        params: WindowManager.LayoutParams,
        onTap: () -> Unit,
        onMoved: (x: Int, y: Int) -> Unit,
        targetViewForWindowUpdate: View = view
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX)
                    val dy = (event.rawY - initialTouchY)
                    if (abs(dx) > CLICK_DRAG_TOLERANCE || abs(dy) > CLICK_DRAG_TOLERANCE) dragging = true
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    runCatching { windowManager.updateViewLayout(targetViewForWindowUpdate, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) onMoved(params.x, params.y) else onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun overlayLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun coloredCircle(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun colorForHero(heroId: Int): Int {
        val palette = intArrayOf(
            Color.parseColor("#4C8DFF"), Color.parseColor("#FF4D5E"),
            Color.parseColor("#FFB238"), Color.parseColor("#B07CFF"),
            Color.parseColor("#2FD883"), Color.parseColor("#3FBFB0")
        )
        return palette[abs(heroId) % palette.size]
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun simpleWatcher(onChange: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: android.text.Editable?) { onChange(s?.toString().orEmpty()) }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "CounterPick overlay", NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Keeps the floating counter-pick bubble running" }
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CounterPick overlay is running")
            .setContentText("Tap the bubble over your game to pick counters. Tap ✕ in the panel to stop.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 4201
        private const val MAX_SLOTS = 5
        private const val CLICK_DRAG_TOLERANCE = 12f
        private const val BUBBLE_SIZE_DP = 62
        private const val PANEL_WIDTH_DP = 288
    }
}

private class HeroRowAdapter(
    private val context: Context,
    private val heroes: List<HeroEntity>
) : android.widget.BaseAdapter() {
    override fun getCount() = heroes.size
    override fun getItem(position: Int) = heroes[position]
    override fun getItemId(position: Int) = heroes[position].id.toLong()

    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.overlay_hero_row, parent, false)
        val hero = heroes[position]
        view.findViewById<TextView>(R.id.hero_row_name).text = hero.name
        val initial = view.findViewById<TextView>(R.id.hero_row_initial)
        initial.text = hero.name.take(1).uppercase()
        initial.visibility = View.VISIBLE

        val image = view.findViewById<ImageView>(R.id.hero_row_image)
        image.visibility = View.GONE
        image.clipToOutline = true
        image.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
        }
        if (!hero.image.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(hero.image)
                .target(object : Target {
                    override fun onSuccess(result: Drawable) {
                        image.setImageDrawable(result)
                        image.visibility = View.VISIBLE
                        initial.visibility = View.GONE
                    }
                    override fun onError(error: Drawable?) {  }
                })
                .build()
            runCatching { Coil.imageLoader(context).enqueue(request) }
        }
        return view
    }
}
