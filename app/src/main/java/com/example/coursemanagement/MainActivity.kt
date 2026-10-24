package com.example.coursemanagement

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.coursemanagement.data.Category
import com.example.coursemanagement.data.Course
import com.example.coursemanagement.ui.theme.CourseManagementTheme
import com.example.coursemanagement.viewmodel.ConnectionState
import com.example.coursemanagement.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourseManagementTheme {
                val vm: MainViewModel = viewModel()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val state = vm.connectionState) {
                        is ConnectionState.Loading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is ConnectionState.Error -> {
                            ErrorScreen(message = state.message, onRetry = { vm.fetchData() })
                        }
                        is ConnectionState.Success -> {
                            MainAppScreen(vm = vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Lỗi",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Thử lại (Retry)") }
    }
}

// Lớp đại diện cho các tab ở thanh điều hướng dưới
sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Courses : BottomNavItem("courses", "Khóa học", Icons.Default.PlayArrow)
    object Categories : BottomNavItem("categories", "Danh mục", Icons.Default.List)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(vm: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Danh sách các màn hình chính để hiển thị Bottom Navigation & FAB
    val mainRoutes = listOf(BottomNavItem.Courses.route, BottomNavItem.Categories.route)
    val isMainScreen = currentRoute in mainRoutes

    Scaffold(
        topBar = {
            if (isMainScreen) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (currentRoute == BottomNavItem.Courses.route) "Quản lý Khóa học" else "Quản lý Danh mục",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (isMainScreen) {
                NavigationBar {
                    val items = listOf(BottomNavItem.Courses, BottomNavItem.Categories)
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isMainScreen) {
                FloatingActionButton(
                    onClick = {
                        if (currentRoute == BottomNavItem.Courses.route) {
                            navController.navigate("addCourse")
                        } else {
                            navController.navigate("addCategory")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm mới")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Courses.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Courses.route) { CourseListScreen(vm, navController) }
            composable(BottomNavItem.Categories.route) { CategoryListScreen(vm, navController) }

            composable("courseDetail/{id}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("id")
                CourseDetailScreen(vm, courseId, navController)
            }

            // Routing cho Form Thêm/Sửa
            composable("addCategory") { CategoryFormScreen(vm, null, navController) }
            composable("editCategory/{id}") { backStackEntry ->
                val catId = backStackEntry.arguments?.getString("id")
                CategoryFormScreen(vm, catId, navController)
            }

            composable("addCourse") { CourseFormScreen(vm, null, navController) }
            composable("editCourse/{id}") { backStackEntry ->
                val courseId = backStackEntry.arguments?.getString("id")
                CourseFormScreen(vm, courseId, navController)
            }
        }
    }
}

// ---------------- SCREENS HIỂN THỊ DANH SÁCH ----------------

@Composable
fun CourseListScreen(vm: MainViewModel, navController: NavHostController) {
    if (vm.courses.isEmpty()) {
        EmptyStateMessage("Chưa có khóa học nào.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(vm.courses) { course ->
            val formatVNĐ = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("courseDetail/${course.id}") },
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Row(Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = course.thumbnail,
                        contentDescription = "Hình thu nhỏ khóa học",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "GV: ${course.instructor}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatVNĐ.format(course.price),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(vm: MainViewModel, courseId: String?, navController: NavHostController) {
    val course = vm.courses.find { it.id == courseId }
    val category = vm.categories.find { it.id == course?.categoryId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (course == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết khóa học", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Video Player 16:9
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AndroidView(factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(course.trailerUrl))
                        setMediaController(MediaController(ctx).apply { setAnchorView(this@apply) })
                        start()
                    }
                }, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(16.dp))
            Text(course.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            SuggestionChip(
                onClick = { },
                label = { Text(category?.name ?: "Danh mục không xác định") }
            )

            Spacer(Modifier.height(16.dp))
            Text("Giảng viên: ${course.instructor}", style = MaterialTheme.typography.bodyLarge)
            Text("Thời lượng: ${course.duration} phút", style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(8.dp))
            Text("Mô tả:", fontWeight = FontWeight.Bold)
            Text(course.description, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { navController.navigate("editCourse/${course.id}") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sửa")
                }
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Xóa")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc chắn muốn xóa khóa học '${course.title}' không?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCourse(course.id)
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun CategoryListScreen(vm: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    if (vm.categories.isEmpty()) {
        EmptyStateMessage("Chưa có danh mục nào.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(vm.categories) { cat ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cat.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (cat.description.isNotBlank()) {
                            Text(text = cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row {
                        IconButton(onClick = { navController.navigate("editCategory/${cat.id}") }) {
                            Icon(Icons.Default.Edit, contentDescription = "Sửa")
                        }
                        IconButton(onClick = { categoryToDelete = cat.id }) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    categoryToDelete?.let { id ->
        val catName = vm.categories.find { it.id == id }?.name
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Xóa danh mục") },
            text = { Text("Bạn có chắc chắn muốn xóa danh mục '$catName'? Lỗi sẽ xảy ra nếu danh mục này đang chứa khóa học.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCategory(id) {
                        Toast.makeText(context, "Lỗi: Không thể xóa vì đang có khóa học thuộc danh mục này!", Toast.LENGTH_LONG).show()
                    }
                    categoryToDelete = null
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------- SCREENS FORM NHẬP LIỆU (THÊM / SỬA) ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(vm: MainViewModel, categoryId: String?, navController: NavHostController) {
    val isEdit = categoryId != null
    val initialCategory = if (isEdit) vm.categories.find { it.id == categoryId } else null

    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var description by remember { mutableStateOf(initialCategory?.description ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Sửa Danh Mục" else "Thêm Danh Mục") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên danh mục") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val cat = Category(id = categoryId ?: "", name = name, description = description)
                    if (isEdit) {
                        vm.updateCategory(categoryId!!, cat) { navController.popBackStack() }
                    } else {
                        vm.addCategory(cat) { navController.popBackStack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Lưu Danh Mục")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseFormScreen(vm: MainViewModel, courseId: String?, navController: NavHostController) {
    val isEdit = courseId != null
    val initialCourse = if (isEdit) vm.courses.find { it.id == courseId } else null

    var title by remember { mutableStateOf(initialCourse?.title ?: "") }
    var description by remember { mutableStateOf(initialCourse?.description ?: "") }
    var instructor by remember { mutableStateOf(initialCourse?.instructor ?: "") }
    var price by remember { mutableStateOf(initialCourse?.price?.toString() ?: "") }
    var duration by remember { mutableStateOf(initialCourse?.duration?.toString() ?: "") }
    var thumbnail by remember { mutableStateOf(initialCourse?.thumbnail ?: "") }
    var trailerUrl by remember { mutableStateOf(initialCourse?.trailerUrl ?: "") }

    // Xử lý Category Dropdown
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(vm.categories.find { it.id == initialCourse?.categoryId } ?: vm.categories.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Sửa Khóa Học" else "Thêm Khóa Học") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Tiêu đề khóa học") }, modifier = Modifier.fillMaxWidth()
            )

            // Dropdown chọn danh mục
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(selectedCategory?.name ?: "Chọn danh mục")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    vm.categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = { selectedCategory = cat; expanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = instructor, onValueChange = { instructor = it },
                label = { Text("Giảng viên") }, modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Giá (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = duration, onValueChange = { duration = it },
                    label = { Text("Phút") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = thumbnail, onValueChange = { thumbnail = it },
                label = { Text("Link ảnh thu nhỏ (Thumbnail Url)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = trailerUrl, onValueChange = { trailerUrl = it },
                label = { Text("Link Video (Trailer Url)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Mô tả") }, modifier = Modifier.fillMaxWidth(), minLines = 3
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val course = Course(
                        id = courseId ?: "",
                        title = title,
                        description = description,
                        categoryId = selectedCategory?.id ?: "",
                        instructor = instructor,
                        price = price.toLongOrNull() ?: 0L,
                        duration = duration.toIntOrNull() ?: 0,
                        thumbnail = thumbnail,
                        trailerUrl = trailerUrl
                    )
                    if (isEdit) {
                        vm.updateCourse(courseId!!, course) { navController.popBackStack() }
                    } else {
                        vm.addCourse(course) { navController.popBackStack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && selectedCategory != null
            ) {
                Text("Lưu Khóa Học")
            }
        }
    }
}