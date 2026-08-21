package com.sevapath.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.roundToInt

private val Ink = Color(0xFF1A1C1E)
private val Forest = Color(0xFF004D40)
private val Mint = Color(0xFFE0F2F1)
private val Cream = Color(0xFFFDFDF5)
private val Gold = Color(0xFFFFC107)
private val Coral = Color(0xFFFF5722)
private val Slate = Color(0xFF455A64)
private val RetroBlue = Color(0xFF00BCD4)
private val RetroPurple = Color(0xFF9C27B0)
private val AtmosphereTop = Color(0xFF071C2D)
private val AtmosphereMid = Color(0xFF0B4255)
private val AtmosphereBottom = Color(0xFF176B70)
private val GlassAqua = Color(0xFF8CF2E5)
private val GlassSky = Color(0xFF83CFF5)
private val GlassText = Color(0xFFF4FFFD)
private val GlassMuted = Color(0xFFC5E1E1)

// Glassmorphism effect helper
@Composable
fun Modifier.glassmorphic(
    backgroundColor: Color = Color.White.copy(alpha = 0.45f),
    edgeColor: Color = Color.White.copy(alpha = 0.5f)
): Modifier = this
    .background(backgroundColor)
    .border(1.dp, edgeColor, RoundedCornerShape(24.dp))
    .clip(RoundedCornerShape(24.dp))

private fun Modifier.liquidGlass(
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    tint: Color = Color.White.copy(alpha = 0.10f)
): Modifier = this
    .shadow(18.dp, shape, ambientColor = Color.Black.copy(alpha = 0.20f), spotColor = GlassAqua.copy(alpha = 0.16f))
    .clip(shape)
    .background(
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                tint,
                GlassSky.copy(alpha = 0.06f)
            )
        )
    )
    .border(
        1.dp,
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.58f), Color.White.copy(alpha = 0.10f))),
        shape
    )

private val RetroMono = FontFamily.Monospace

data class CitizenRequest(
    val title: String,
    val location: String,
    val category: String,
    val status: String,
    val statusColor: Color,
    val evidence: String
)

data class NewsItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val timestamp: String,
    val likes: Int,
    val likedByMe: Boolean = false
)

private fun CitizenRequest.toJson(): JSONObject = JSONObject()
    .put("title", title)
    .put("location", location)
    .put("category", category)
    .put("status", status)
    .put("evidence", evidence)

private fun NewsItem.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("body", body)
    .put("type", type)
    .put("timestamp", timestamp)
    .put("likes", likes)
    .put("likedByMe", likedByMe)

private fun JSONArray?.toRequestList(): List<CitizenRequest> = buildList {
    if (this@toRequestList == null) return@buildList
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        val status = item.optString("status")
        add(
            CitizenRequest(
                title = item.optString("title"),
                location = item.optString("location"),
                category = item.optString("category"),
                status = status,
                statusColor = when (status) {
                    "Under review" -> Gold
                    "Verified", "Received" -> Forest
                    else -> Coral
                },
                evidence = item.optString("evidence")
            )
        )
    }
}

private fun JSONArray?.toNewsList(): List<NewsItem> = buildList {
    if (this@toNewsList == null) return@buildList
    for (index in 0 until length()) {
        val item = optJSONObject(index) ?: continue
        add(
            NewsItem(
                id = item.optString("id"),
                title = item.optString("title"),
                body = item.optString("body"),
                type = item.optString("type"),
                timestamp = item.optString("timestamp"),
                likes = item.optInt("likes"),
                likedByMe = item.optBoolean("likedByMe")
            )
        )
    }
}

data class Hotspot(
    val name: String,
    val category: String,
    val score: Int,
    val affected: String,
    val explanation: String
)

data class PriorityFactors(
    val demand: Double,
    val infrastructureGap: Double,
    val vulnerability: Double,
    val schemeAlignment: Double,
    val urgency: Double,
    val feasibility: Double,
    val existingCoverage: Double
)

object PriorityScorer {
    fun score(factors: PriorityFactors): Int = (
        0.30 * factors.demand +
            0.25 * factors.infrastructureGap +
            0.15 * factors.vulnerability +
            0.15 * factors.schemeAlignment +
            0.10 * factors.urgency +
            0.05 * factors.feasibility -
            0.10 * factors.existingCoverage
        ).coerceIn(0.0, 1.0).times(100).roundToInt()
}

class CivicViewModel : ViewModel() {
    private var database: SevaPathDb? = null
    private val accountPasswordDigests = mutableMapOf<String, String>()
    private val accountSalts = mutableMapOf<String, String>()
    private val accountNames = mutableMapOf("demo@sevapath.app" to "Demo Citizen")
    private val accountRequests = mutableMapOf<String, List<CitizenRequest>>()
    private val accountNews = mutableMapOf<String, List<NewsItem>>()
    // Legacy JSON store used only to migrate accounts created before SQLite was added.
    private var storage: SharedPreferences? = null

    var storageReady by mutableStateOf(false)
        private set

    var currentUserEmail by mutableStateOf<String?>(null)
        private set
    var currentUserName by mutableStateOf("")
        private set
    var authMode by mutableStateOf("Login")
    var authName by mutableStateOf("")
    var authEmail by mutableStateOf("")
    var authPassword by mutableStateOf("")
    var authError by mutableStateOf("")

    var selectedTab by mutableStateOf(0)
    var language by mutableStateOf("Hindi")
    var issueType by mutableStateOf("Drinking water")
    var description by mutableStateOf("")
    var isRecording by mutableStateOf(false)
    var submitted by mutableStateOf(false)
        private set

    var requests by mutableStateOf(emptyList<CitizenRequest>())
        private set
    var news by mutableStateOf(emptyList<NewsItem>())
        private set

    val hotspots = listOf(
        Hotspot("Bhairavpur · Ward 4", "Drinking water", PriorityScorer.score(PriorityFactors(.98, .95, .92, .82, .88, .70, .18)), "1,840 people", "High demand, low asset coverage and seasonal vulnerability."),
        Hotspot("Bhairavpur · Ward 7", "Street lighting", PriorityScorer.score(PriorityFactors(.82, .75, .63, .72, .76, .82, .22)), "960 people", "Repeated reports near a clinic and school corridor."),
        Hotspot("Bhairavpur · Ward 2", "Road connectivity", PriorityScorer.score(PriorityFactors(.76, .70, .58, .90, .62, .78, .31)), "1,210 people", "Demand aligns with an existing rural road upgrade scheme.")
    )

    init {
        accountSalts["demo@sevapath.app"] = "sevapath-demo-salt"
        accountPasswordDigests["demo@sevapath.app"] = digestPassword("demo123", "sevapath-demo-salt")
        accountRequests["demo@sevapath.app"] = listOf(
            CitizenRequest("New water pipeline", "Ward 4 · Bhairavpur", "Water", "Under review", Gold, "43 similar requests"),
            CitizenRequest("Repair the school road", "Ward 2 · Bhairavpur", "Roads", "Verified", Forest, "18 similar requests"),
            CitizenRequest("Streetlights near the clinic", "Ward 7 · Bhairavpur", "Safety", "Planned", Coral, "27 similar requests")
        )
        accountNews["demo@sevapath.app"] = listOf(
            NewsItem("demo-water", "Water requests are trending", "43 similar requests are grouped around Ward 4.", "WATER", "12 min ago", 34),
            NewsItem("demo-road", "Road repair moved to verified", "The Ward 2 request now matches a public works signal.", "ROADS", "1 hr ago", 18),
            NewsItem("demo-light", "Streetlight reports gaining attention", "Repeated reports are clustering near the clinic corridor.", "SAFETY", "3 hr ago", 26)
        )
    }

    fun attachStorage(context: Context) {
        if (database != null) return
        storage = context.getSharedPreferences("sevapath_local_store", Context.MODE_PRIVATE)
        restoreStorage()
        val db = SevaPathDb(context.applicationContext)
        if (!db.userExists("demo@sevapath.app")) {
            db.createUser("Demo Citizen", "demo@sevapath.app", "demo123")
            db.saveRequests("demo@sevapath.app", accountRequests["demo@sevapath.app"].orEmpty())
            db.saveNews("demo@sevapath.app", accountNews["demo@sevapath.app"].orEmpty())
        }
        database = db
        storageReady = true
    }

    fun login(email: String = authEmail, password: String = authPassword): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val persistentDb = database
        val userExists = persistentDb?.userExists(normalizedEmail) ?: accountPasswordDigests.containsKey(normalizedEmail)
        val authenticated = if (persistentDb != null) {
            persistentDb.authenticate(normalizedEmail, password) != null ||
                (!persistentDb.userExists(normalizedEmail) && matchesPassword(normalizedEmail, password))
        } else {
            matchesPassword(normalizedEmail, password)
        }
        authError = when {
            normalizedEmail.isBlank() || password.isBlank() -> "Enter email and password."
            !userExists -> "Account not found. Create an account first."
            !authenticated -> "Password does not match this account."
            else -> ""
        }
        if (authError.isNotEmpty()) return false
        if (persistentDb != null && !persistentDb.userExists(normalizedEmail)) {
            persistentDb.createUser(accountNames[normalizedEmail].orEmpty().ifBlank { "Citizen" }, normalizedEmail, password)
            persistentDb.saveRequests(normalizedEmail, accountRequests[normalizedEmail].orEmpty())
            persistentDb.saveNews(normalizedEmail, accountNews[normalizedEmail].orEmpty())
        }
        loadAccount(normalizedEmail)
        return true
    }

    fun signup(name: String = authName, email: String = authEmail, password: String = authPassword): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val persistentDb = database
        authError = when {
            name.trim().isBlank() -> "Enter your name."
            !normalizedEmail.contains("@") -> "Enter a valid email address."
            password.length < 6 -> "Use at least 6 characters for the password."
            (persistentDb?.userExists(normalizedEmail) ?: accountPasswordDigests.containsKey(normalizedEmail)) -> "This email already has an account."
            else -> ""
        }
        if (authError.isNotEmpty()) return false
        if (persistentDb != null) {
            if (!persistentDb.createUser(name.trim(), normalizedEmail, password)) {
                authError = "This email already has an account."
                return false
            }
            accountNames[normalizedEmail] = name.trim()
            loadAccount(normalizedEmail)
            return true
        }
        val salt = UUID.randomUUID().toString()
        accountSalts[normalizedEmail] = salt
        accountPasswordDigests[normalizedEmail] = digestPassword(password, salt)
        accountNames[normalizedEmail] = name.trim()
        accountRequests[normalizedEmail] = emptyList()
        accountNews[normalizedEmail] = emptyList()
        persistStorage()
        loadAccount(normalizedEmail)
        return true
    }

    fun logout() {
        persistAccount()
        currentUserEmail = null
        currentUserName = ""
        requests = emptyList()
        news = emptyList()
        authPassword = ""
        authError = ""
        authMode = "Login"
        persistStorage()
    }

    private fun loadAccount(email: String) {
        currentUserEmail = email
        currentUserName = database?.userName(email) ?: accountNames[email].orEmpty()
        requests = database?.loadRequests(email) ?: accountRequests[email].orEmpty()
        news = database?.loadNews(email) ?: accountNews[email].orEmpty()
        selectedTab = 0
        authPassword = ""
        authError = ""
    }

    private fun persistAccount() {
        currentUserEmail?.let { email ->
            accountRequests[email] = requests
            accountNews[email] = news
            database?.saveRequests(email, requests)
            database?.saveNews(email, news)
        }
        persistStorage()
    }

    fun submitRequest() {
        if (description.isBlank()) return
        val title = description.trim()
        requests = listOf(CitizenRequest(title, "Bhairavpur · Your location", issueType, "Received", Forest, "New request")) + requests
        news = listOf(
            NewsItem("request-${System.nanoTime()}", "New $issueType request recorded", "Your report is now part of the local civic signal.", issueType.uppercase(), "just now", 0)
        ) + news
        persistAccount()
        description = ""
        submitted = true
    }

    fun toggleLike(itemId: String) {
        news = news.map { item ->
            if (item.id != itemId) item
            else item.copy(likes = (item.likes + if (item.likedByMe) -1 else 1).coerceAtLeast(0), likedByMe = !item.likedByMe)
        }
        persistAccount()
    }

    private fun matchesPassword(email: String, password: String): Boolean =
        accountSalts[email]?.let { salt -> digestPassword(password, salt) == accountPasswordDigests[email] } == true

    private fun digestPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun persistStorage() {
        val preferences = storage ?: return
        val accounts = JSONObject()
        accountPasswordDigests.keys.forEach { email ->
            val account = JSONObject()
                .put("name", accountNames[email].orEmpty())
                .put("salt", accountSalts[email].orEmpty())
                .put("passwordDigest", accountPasswordDigests[email].orEmpty())
                .put("requests", JSONArray().apply { accountRequests[email].orEmpty().forEach { put(it.toJson()) } })
                .put("news", JSONArray().apply { accountNews[email].orEmpty().forEach { put(it.toJson()) } })
            accounts.put(email, account)
        }
        preferences.edit().putString("accounts", accounts.toString()).apply()
    }

    private fun restoreStorage() {
        val raw = storage?.getString("accounts", null) ?: return
        runCatching {
            val accounts = JSONObject(raw)
            accounts.keys().forEach { email ->
                val account = accounts.getJSONObject(email)
                accountNames[email] = account.optString("name")
                accountSalts[email] = account.optString("salt")
                accountPasswordDigests[email] = account.optString("passwordDigest")
                accountRequests[email] = account.optJSONArray("requests").toRequestList()
                accountNews[email] = account.optJSONArray("news").toNewsList()
            }
        }
    }

    fun clearSubmitted() { submitted = false }

    override fun onCleared() {
        database?.close()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SevaPathApp() }
    }
}

@Composable
fun SevaPathApp(vm: CivicViewModel = viewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.attachStorage(context.applicationContext) }

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            background = Color.Transparent,
            surface = Color.White.copy(alpha = 0.7f),
            onSurface = Ink,
            secondary = RetroBlue
        ),
        typography = MaterialTheme.typography.copy(
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = RetroMono, fontWeight = FontWeight.Bold)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!vm.storageReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("LOADING_SAVED_ACCOUNT", color = Forest, fontFamily = RetroMono, fontWeight = FontWeight.Bold)
                }
            } else {
                // Animated/Vibrant background blobs for Glassmorphism
                Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Mint, Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.2f),
                            radius = size.width * 0.8f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(RetroBlue.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.5f),
                            radius = size.width * 0.6f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Coral.copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.8f),
                            radius = size.width * 0.7f
                        )
                    )
                }

                if (vm.currentUserEmail == null) {
                    AuthScreen(vm)
                } else {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = { AppTopBar(vm.selectedTab) },
                        bottomBar = { BottomNav(vm) }
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            when (vm.selectedTab) {
                                0 -> HomeScreen(vm)
                                1 -> ReportScreen(vm)
                                2 -> InsightsScreen(vm)
                                3 -> NewsScreen(vm)
                                else -> TrackScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(vm: CivicViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("SEVAPATH", color = Forest, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = RetroMono)
        Text("Your voice belongs in the next public investment decision.", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 29.sp)
        Text("Create a fresh civic profile or sign in to continue your trail.", color = Slate)

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Login", "Sign up").forEach { mode ->
                val selected = vm.authMode == mode
                TextButton(
                    onClick = { vm.authMode = mode; vm.authError = "" },
                    modifier = Modifier.weight(1f).background(if (selected) Ink else Color.Transparent, RoundedCornerShape(10.dp))
                ) { Text(mode.uppercase(), color = if (selected) Color.White else Ink, fontFamily = RetroMono, fontWeight = FontWeight.Bold) }
            }
        }

        if (vm.authMode == "Sign up") {
            OutlinedTextField(
                value = vm.authName,
                onValueChange = { vm.authName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("YOUR NAME") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
        }
        OutlinedTextField(
            value = vm.authEmail,
            onValueChange = { vm.authEmail = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("EMAIL") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp)
        )
        OutlinedTextField(
            value = vm.authPassword,
            onValueChange = { vm.authPassword = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PASSWORD") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp)
        )
        if (vm.authError.isNotEmpty()) {
            Text(vm.authError, color = Coral, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { if (vm.authMode == "Login") vm.login() else vm.signup() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Forest),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (vm.authMode == "Login") "LOGIN" else "CREATE_FRESH_ACCOUNT", fontWeight = FontWeight.Black, fontFamily = RetroMono) }

        if (vm.authMode == "Login") {
            Text("Demo access: demo@sevapath.app  ·  demo123", color = Slate, style = MaterialTheme.typography.bodySmall, fontFamily = RetroMono)
        } else {
            Text("New accounts begin with 0 requests, 0 likes and 0 personal news items.", color = Slate, style = MaterialTheme.typography.bodySmall)
        }
        Text("Local prototype account flow · connect a secure backend before production.", color = Slate.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun AppTopBar(tab: Int) {
    TopAppBar(
        title = {
            Column {
                Text("SEVAPATH", fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontFamily = RetroMono)
                Text(
                    when (tab) { 1 -> "VOICE_YOUR_NEEDS"; 2 -> "DATA_DRIVEN_LOGIC"; 3 -> "LIVE_CIVIC_NEWS"; 4 -> "CIVIC_HISTORY"; else -> "COMMUNITY_PRIORITIES" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate,
                    letterSpacing = 0.5.sp
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.5f))
    )
}

@Composable
private fun BottomNav(vm: CivicViewModel) {
    val items = listOf("Home" to "HOME", "Report" to "NEW", "Insights" to "MAP", "News" to "FEED", "Track" to "LOG")
    NavigationBar(
        containerColor = Color.White.copy(alpha = 0.8f),
        modifier = Modifier.navigationBarsPadding().padding(12.dp).clip(RoundedCornerShape(32.dp)).border(2.dp, Ink, RoundedCornerShape(32.dp))
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = vm.selectedTab == index,
                onClick = { vm.selectedTab = index },
                icon = { Text(item.first.take(1), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = RetroMono) },
                label = { Text(item.second, fontFamily = RetroMono, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Forest,
                    selectedTextColor = Forest,
                    indicatorColor = Mint,
                    unselectedIconColor = Slate,
                    unselectedTextColor = Slate
                )
            )
        }
    }
}

@Composable
private fun CivicAtmosphere(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AtmosphereTop, AtmosphereMid, AtmosphereBottom)))
    ) {
        Canvas(Modifier.fillMaxSize().blur(54.dp)) {
            drawCircle(
                brush = Brush.radialGradient(listOf(GlassSky.copy(alpha = 0.34f), Color.Transparent)),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.88f, size.height * 0.10f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(GlassAqua.copy(alpha = 0.24f), Color.Transparent)),
                radius = size.width * 0.62f,
                center = Offset(size.width * 0.10f, size.height * 0.62f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(RetroPurple.copy(alpha = 0.18f), Color.Transparent)),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.82f, size.height * 0.88f)
            )
        }
        content()
    }
}

@Composable
private fun GlassPill(label: String, color: Color) {
    Text(
        label,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = RetroMono,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 7.dp)
    )
}

@Composable
private fun HomeScreen(vm: CivicViewModel) {
    CivicAtmosphere {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 120.dp)
        ) {
            item { WelcomeCard(vm) }
            item { AccountStrip(vm) }
            item { QuickActions(vm) }
            item { LiquidSectionHeader("Live civic signal", "Updated just now") }
            item { SignalCard(vm) }
            item { LiquidSectionHeader("Priority near you", "${vm.hotspots.size} hotspots") }
            item { PriorityPreview(vm.hotspots.first()) }
        }
    }
}

@Composable
private fun WelcomeCard(vm: CivicViewModel) {
    Box(Modifier.fillMaxWidth().liquidGlass(tint = GlassAqua.copy(alpha = 0.10f))) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("BHAIRAVPUR · CIVIC DASHBOARD", color = GlassAqua, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono, modifier = Modifier.weight(1f))
                Box(Modifier.size(9.dp).clip(CircleShape).background(GlassAqua))
            }
            Spacer(Modifier.height(22.dp))
            Text("Good morning,", color = GlassMuted, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(vm.currentUserName.ifBlank { "Citizen" }, color = GlassText, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(10.dp))
            Text("Your local knowledge can shape the next public investment.", color = GlassText.copy(alpha = 0.90f), fontSize = 17.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassPill("●  SIGNAL ACTIVE", GlassAqua)
                Text("${vm.requests.size} filed", color = GlassMuted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AccountStrip(vm: CivicViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().liquidGlass(RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(GlassAqua.copy(alpha = 0.16f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
            Text(vm.currentUserName.take(1).uppercase().ifBlank { "S" }, color = GlassAqua, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(vm.currentUserEmail.orEmpty(), color = GlassMuted, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
            Text("${vm.requests.size} requests · ${vm.news.sumOf { it.likes }} likes", color = GlassText, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = { vm.logout() }) { Text("LOG OUT", color = Color(0xFFFFB5A4), fontFamily = RetroMono, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun QuickActions(vm: CivicViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ActionCard("＋", "Report", "Voice or text") { vm.selectedTab = 1 }
        ActionCard("◉", "Hotspots", "See priorities") { vm.selectedTab = 2 }
        ActionCard("✓", "Track", "Follow progress") { vm.selectedTab = 4 }
    }
}

@Composable
private fun RowScope.ActionCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(116.dp)
            .liquidGlass(RoundedCornerShape(24.dp))
            .semantics { contentDescription = "$title, $subtitle" }
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(15.dp)) {
            Box(Modifier.size(34.dp).background(Color.White.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Text(icon, color = GlassAqua, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(9.dp))
            Text(title, color = GlassText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = GlassMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LiquidSectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = GlassText, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Text(action, color = GlassAqua, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Text(action, color = Forest, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SignalCard(vm: CivicViewModel) {
    Box(modifier = Modifier.fillMaxWidth().liquidGlass()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(vm.requests.size.toString(), fontSize = 46.sp, fontWeight = FontWeight.Light, color = GlassText)
                Spacer(Modifier.width(8.dp))
                Text("YOUR REQUESTS", color = GlassMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 9.dp), fontFamily = RetroMono)
            }
            Spacer(Modifier.height(16.dp))
            SimpleTrendChart(GlassAqua)
            Spacer(Modifier.height(12.dp))
            Text(
                if (vm.requests.isEmpty()) "Your first report will start a local civic signal."
                else "${vm.requests.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key.orEmpty()} is your strongest recorded signal.",
                color = GlassMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SimpleTrendChart(lineColor: Color = Forest) {
    Canvas(Modifier.fillMaxWidth().height(62.dp)) {
        val points = listOf(0.72f, 0.48f, 0.58f, 0.35f, 0.42f, 0.18f, 0.28f, 0.1f)
        val path = Path()
        points.forEachIndexed { index, value ->
            val x = size.width * index / (points.size - 1)
            val y = size.height * value
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
        points.forEachIndexed { index, value ->
            drawCircle(lineColor, 5f, Offset(size.width * index / (points.size - 1), size.height * value))
        }
    }
}

@Composable
private fun PriorityPreview(hotspot: Hotspot) {
    Box(modifier = Modifier.fillMaxWidth().liquidGlass(tint = GlassSky.copy(alpha = 0.10f))) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hotspot.category.uppercase(), color = GlassAqua, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
                Spacer(Modifier.height(4.dp))
                Text(hotspot.name, color = GlassText, fontWeight = FontWeight.SemiBold, fontSize = 21.sp)
                Text("${hotspot.affected} AFFECTED", color = GlassMuted, style = MaterialTheme.typography.labelSmall)
            }
            Box(Modifier.size(68.dp).background(Gold.copy(alpha = 0.92f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hotspot.score.toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 21.sp)
                    Text("SCORE", color = Ink.copy(alpha = 0.72f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .background(Ink, CircleShape)
                .padding(2.dp)
                .background(Gold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(score.toString(), color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp, fontFamily = RetroMono)
        }
        Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = Ink)
    }
}

@Composable
private fun ReportScreen(vm: CivicViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)); Text("What needs attention?", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item { LanguageSelector(vm) }
        item { VoiceCapture(vm) }
        item { IssueTypes(vm) }
        item { DescriptionField(vm) }
        item { LocationCard() }
        item { SubmitButton(vm) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun LanguageSelector(vm: CivicViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("LANGUAGE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        val languages = listOf("Hindi", "English", "Odia")
        languages.forEach { lang ->
            FilterChip(
                selected = vm.language == lang,
                onClick = { vm.language = lang },
                label = { Text(lang, fontFamily = RetroMono) },
                modifier = Modifier.padding(start = 4.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Ink,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = true, borderColor = Ink, borderWidth = 2.dp)
            )
        }
    }
}

@Composable
private fun VoiceCapture(vm: CivicViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Ink, RoundedCornerShape(24.dp))
            .glassmorphic(backgroundColor = if (vm.isRecording) Coral.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f))
            .semantics { contentDescription = "Record a voice request" }
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(58.dp)
                    .background(Ink, CircleShape)
                    .padding(2.dp)
                    .background(if (vm.isRecording) Coral else Mint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (vm.isRecording) "STOP" else "REC", color = if (vm.isRecording) Color.White else Forest, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = RetroMono)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (vm.isRecording) "LISTENING..." else "VOICE_INPUT", fontWeight = FontWeight.Black, fontFamily = RetroMono)
                Text(if (vm.isRecording) "TAP STOP" else "MULTILINGUAL_AI", color = Slate, style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { vm.isRecording = !vm.isRecording },
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(if (vm.isRecording) "OFF" else "ON", fontFamily = RetroMono, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IssueTypes(vm: CivicViewModel) {
    Column {
        Text("Issue type", fontWeight = FontWeight.Bold)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Drinking water", "Roads", "Streetlights", "Health").forEach { type ->
                FilterChip(selected = vm.issueType == type, onClick = { vm.issueType = type }, label = { Text(type) })
            }
        }
    }
}

@Composable
private fun DescriptionField(vm: CivicViewModel) {
    OutlinedTextField(
        value = vm.description,
        onValueChange = { vm.description = it },
        modifier = Modifier.fillMaxWidth().height(140.dp).border(2.dp, Ink, RoundedCornerShape(16.dp)),
        label = { Text("DESCRIPTION", fontFamily = RetroMono, fontSize = 12.sp) },
        placeholder = { Text("E.g. No water since Tuesday...", color = Ink.copy(alpha = 0.5f)) },
        maxLines = 4,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = RetroBlue
        )
    )
}

@Composable
private fun LocationCard() {
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(18.dp)).glassmorphic()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("GPS", fontSize = 18.sp, color = Coral, fontWeight = FontWeight.Black, fontFamily = RetroMono)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("WARD_04 · BHAIRAVPUR", fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("ENCRYPTED_SIGNAL", color = Slate, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = {}) { Text("EDIT", fontFamily = RetroMono, color = Forest, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SubmitButton(vm: CivicViewModel) {
    Column {
        Button(
            onClick = { vm.submitRequest() },
            enabled = vm.description.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(58.dp).border(3.dp, Ink, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (vm.description.isNotBlank()) RetroBlue else Slate, disabledContainerColor = Slate.copy(alpha = 0.3f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
        ) { Text("SUBMIT_REPORT", fontWeight = FontWeight.Black, fontFamily = RetroMono, letterSpacing = 1.sp) }
        if (vm.submitted) {
            Text("DATA_SENT: GROUPING WITH LOCAL SIGNALS", color = Forest, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun InsightsScreen(vm: CivicViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)); Text("Priority hotspots", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item { Text("A transparent view of where needs, gaps and public plans overlap.", color = Slate) }
        item { HotspotMap(vm.hotspots) }
        item { SectionHeader("Ranked recommendations", "3 areas") }
        items(vm.hotspots, key = { it.name }) { hotspot -> RecommendationCard(hotspot) }
        item { MethodCard() }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HotspotMap(hotspots: List<Hotspot>) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1EC)), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BHAIRAVPUR", color = Forest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Demand density map", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text("● Live", color = Forest, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFFD7E5D9))) {
                Canvas(Modifier.fillMaxSize()) {
                    for (i in 1..5) drawLine(Color(0xFFB5CCBA), Offset(size.width * i / 6, 0f), Offset(size.width * i / 6, size.height), 2f)
                    for (i in 1..4) drawLine(Color(0xFFB5CCBA), Offset(0f, size.height * i / 5), Offset(size.width, size.height * i / 5), 2f)
                    drawLine(Color(0xFF8DAF99), Offset(0f, size.height * .7f), Offset(size.width, size.height * .32f), 7f, cap = StrokeCap.Round)
                    drawCircle(Coral, 18f, Offset(size.width * .27f, size.height * .3f))
                    drawCircle(Gold, 14f, Offset(size.width * .68f, size.height * .66f))
                    drawCircle(Forest, 11f, Offset(size.width * .48f, size.height * .48f))
                }
                Text("W4", Modifier.align(Alignment.TopStart).padding(start = 46.dp, top = 34.dp), color = Color.White, fontWeight = FontWeight.Bold)
                Text("W7", Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 42.dp), color = Ink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("Larger circles mean more repeated requests after population adjustment.", color = Slate, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecommendationCard(hotspot: Hotspot) {
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(24.dp)).glassmorphic()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(hotspot.category.uppercase(), color = Forest, style = MaterialTheme.typography.labelSmall)
                    Text(hotspot.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                ScoreBadge(hotspot.score)
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Ink.copy(alpha = 0.1f), thickness = 1.dp)
            Text(hotspot.explanation, color = Slate, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tag("DEMAND")
                Tag("GAP")
                Tag("PLAN_FIT")
            }
        }
    }
}

@Composable
private fun Tag(label: String) {
    Text(label, color = Ink, style = MaterialTheme.typography.labelSmall, modifier = Modifier.border(2.dp, Ink, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
private fun MethodCard() {
    Box(modifier = Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(24.dp)).padding(20.dp)) {
        Column {
            Text("ALGORITHM_LOGIC", color = Gold, fontWeight = FontWeight.Black, fontFamily = RetroMono, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Text("Demand + Gap + Vulnerability + Alignment - Coverage", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontFamily = RetroMono)
            Spacer(Modifier.height(12.dp))
            Text("Explainable AI: every rank is backed by verifiable civic signals.", color = Slate, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun NewsScreen(vm: CivicViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)); Text("Live civic news", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item { Text("A real-time view of what is happening around your recorded civic activity.", color = Slate) }
        item { NewsSummary(vm) }
        if (vm.news.isEmpty()) {
            item { EmptyNewsCard() }
        } else {
            item { SectionHeader("Latest activity", "Updated just now") }
            items(vm.news, key = { it.id }) { item -> NewsCard(item) { vm.toggleLike(item.id) } }
        }
    }
}

@Composable
private fun NewsSummary(vm: CivicViewModel) {
    val topType = vm.requests.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key ?: "No data yet"
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(24.dp)).background(Mint.copy(alpha = 0.72f), RoundedCornerShape(24.dp))) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ACCOUNT_PULSE", color = Forest, fontWeight = FontWeight.Black, fontFamily = RetroMono, modifier = Modifier.weight(1f))
                Text("● LIVE", color = Forest, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseStat(vm.requests.size.toString(), "REPORTS")
                PulseStat(vm.news.sumOf { it.likes }.toString(), "LIKES")
                PulseStat(topType, "TOP TYPE")
            }
            Spacer(Modifier.height(14.dp))
            Text("Viral means 25 or more likes on a civic update.", color = Slate, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RowScope.PulseStat(value: String, label: String) {
    Column(Modifier.weight(1f)) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, fontFamily = RetroMono, maxLines = 1)
        Text(label, color = Slate, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
    }
}

@Composable
private fun EmptyNewsCard() {
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(24.dp)).glassmorphic()) {
        Column(Modifier.padding(22.dp)) {
            Text("0 PERSONAL UPDATES", color = Forest, fontWeight = FontWeight.Black, fontFamily = RetroMono)
            Spacer(Modifier.height(8.dp))
            Text("Submit your first report to start your civic news trail and receive community likes.", color = Slate, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun NewsCard(item: NewsItem, onLike: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().border(2.dp, Ink, RoundedCornerShape(24.dp)).glassmorphic()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.type, color = Forest, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono, modifier = Modifier.weight(1f))
                if (item.likes >= 25) Tag("VIRAL")
            }
            Spacer(Modifier.height(7.dp))
            Text(item.title, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(item.body, color = Slate, lineHeight = 22.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.timestamp.uppercase(), color = Slate, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onLike, shape = RoundedCornerShape(10.dp)) {
                    Text(if (item.likedByMe) "♥ ${item.likes}" else "♡ ${item.likes}", color = if (item.likedByMe) Coral else Ink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TrackScreen(vm: CivicViewModel) {
    CivicAtmosphere {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            item {
                Text("INDICATION REPORT", color = GlassAqua, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
                Spacer(Modifier.height(6.dp))
                Text("Civic history", color = GlassText, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("Every report stays visible from submission to outcome.", color = GlassMuted, modifier = Modifier.padding(top = 5.dp))
            }
            item { HistorySummary(vm) }
            item { LiquidSectionHeader("Report timeline", "${vm.requests.size} total") }
            if (vm.requests.isEmpty()) item { EmptyTrailCard() } else items(vm.requests, key = { "${it.title}:${it.location}" }) { request -> RequestCard(request) }
            item { PrivacyNote() }
        }
    }
}

@Composable
private fun HistorySummary(vm: CivicViewModel) {
    val verified = vm.requests.count { it.status == "Verified" || it.status == "Planned" }
    val active = vm.requests.count { it.status != "Verified" && it.status != "Planned" }
    Row(
        modifier = Modifier.fillMaxWidth().liquidGlass().padding(horizontal = 18.dp, vertical = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HistoryMetric(vm.requests.size.toString(), "REPORTS")
        HistoryMetric(active.toString(), "ACTIVE")
        HistoryMetric(verified.toString(), "INDICATED")
    }
}

@Composable
private fun RowScope.HistoryMetric(value: String, label: String) {
    Column(Modifier.weight(1f)) {
        Text(value, color = GlassText, fontSize = 27.sp, fontWeight = FontWeight.Light)
        Text(label, color = GlassMuted, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
    }
}

@Composable
private fun EmptyTrailCard() {
    Box(modifier = Modifier.fillMaxWidth().liquidGlass()) {
        Column(Modifier.padding(22.dp)) {
            Text("0 REQUESTS", color = GlassAqua, fontWeight = FontWeight.Black, fontFamily = RetroMono)
            Spacer(Modifier.height(8.dp))
            Text("Your civic trail is ready. Add a report to see its status here.", color = GlassMuted, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun RequestCard(request: CitizenRequest) {
    Box(modifier = Modifier.fillMaxWidth().liquidGlass()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(34.dp)
                        .background(request.statusColor.copy(alpha = 0.20f), CircleShape)
                        .border(1.dp, request.statusColor.copy(alpha = 0.70f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(9.dp).background(request.statusColor, CircleShape))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.category.uppercase(), color = GlassAqua, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
                    Text(request.title, color = GlassText, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp)
                    Text(request.location.uppercase(), color = GlassMuted, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono, modifier = Modifier.padding(top = 4.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.14f), thickness = 1.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassPill(request.status.uppercase(), request.statusColor)
                Spacer(Modifier.width(10.dp))
                Text(request.evidence.uppercase(), color = GlassMuted, style = MaterialTheme.typography.labelSmall, fontFamily = RetroMono)
                Spacer(Modifier.weight(1f))
                Text(
                    "OPEN FILE",
                    color = GlassAqua,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { contentDescription = "Open ${request.title}" }.clickable { }
                )
            }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Text(
        "Privacy by design: precise contact details stay private; only aggregated demand is shown in the planner view.",
        color = GlassMuted,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}
