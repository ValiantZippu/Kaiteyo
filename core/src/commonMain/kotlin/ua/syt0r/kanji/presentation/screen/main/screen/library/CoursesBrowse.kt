@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ua.syt0r.kanji.presentation.screen.main.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import ua.syt0r.kanji.core.knowledge.LibraryCatalog
import ua.syt0r.kanji.core.knowledge.LibraryCollection
import ua.syt0r.kanji.core.knowledge.LibraryCourse
import ua.syt0r.kanji.presentation.common.theme.KaiteyoAccentScheme
import ua.syt0r.kanji.presentation.common.theme.LocalKaiteyoAccent
import ua.syt0r.kanji.presentation.common.theme.LocalSurfaceColors
import ua.syt0r.kanji.presentation.common.theme.SurfaceColors
import ua.syt0r.kanji.presentation.screen.main.MainDestination
import ua.syt0r.kanji.presentation.screen.main.MainNavigationState

// ============================================================
// COURSES BROWSE — courses/lessons as first-class Library
// ------------------------------------------------------------
// The LibraryCatalog model already existed but was never
// surfaced. This lists the REAL courses (Kyōiku Grades 1–6,
// JLPT N5→N1 — every collection is a real dataset query result),
// lets the user drill into a course's lessons, and opens any
// lesson's collection detail. No fabricated content: a course's
// item counts come from the actual catalog.
// ============================================================

@Composable
fun CoursesBrowse(
    navigationState: MainNavigationState,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors
) {
    val catalog = koinInject<LibraryCatalog>()

    var courses by remember { mutableStateOf<List<LibraryCourse>>(emptyList()) }
    var collections by remember { mutableStateOf<Map<String, LibraryCollection>>(emptyMap()) }
    var selectedCourseId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loadedCourses = catalog.courses()
        courses = loadedCourses
        // Resolve every collection referenced by any course once, so the
        // lesson list can show real item counts without per-row queries.
        val resolved = loadedCourses
            .flatMap { it.lessonIds }
            .distinct()
            .mapNotNull { id -> catalog.collection(id) }
            .associateBy { it.id }
        collections = resolved
        loading = false
    }

    val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (selectedCourse == null) "Courses" else "Lessons",
                    color = surfaceColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        loading -> "Loading the catalog…"
                        selectedCourse == null -> "${courses.size} courses · every lesson is a real dataset collection"
                        else -> selectedCourse.description
                    },
                    color = surfaceColors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        when {
            loading -> Text(
                text = "Loading the catalog…",
                color = surfaceColors.textMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(16.dp)
            )
            selectedCourse != null -> LessonList(
                course = selectedCourse,
                collections = collections,
                accent = accent,
                surfaceColors = surfaceColors,
                onBack = { selectedCourseId = null },
                onOpenLesson = { collectionId ->
                    navigationState.navigate(MainDestination.CollectionDetail(collectionId))
                }
            )
            else -> CourseList(
                courses = courses,
                collections = collections,
                accent = accent,
                surfaceColors = surfaceColors,
                onOpenCourse = { selectedCourseId = it }
            )
        }
    }
}

@Composable
private fun CourseList(
    courses: List<LibraryCourse>,
    collections: Map<String, LibraryCollection>,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onOpenCourse: (String) -> Unit
) {
    if (courses.isEmpty()) {
        Text(
            text = "No courses in the bundled catalog.",
            color = surfaceColors.textMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(courses.size) { index ->
            val course = courses[index]
            val lessonCount = course.lessonIds.size
            val itemCount = course.lessonIds.sumOf { collections[it]?.size ?: 0 }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColors.surface)
                    .clickable { onOpenCourse(course.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = course.title,
                        color = surfaceColors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = course.description,
                        color = surfaceColors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$lessonCount lessons · $itemCount items",
                        color = surfaceColors.textMuted,
                        fontSize = 11.sp
                    )
                }
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = surfaceColors.textMuted
                )
            }
        }
    }
}

@Composable
private fun LessonList(
    course: LibraryCourse,
    collections: Map<String, LibraryCollection>,
    accent: KaiteyoAccentScheme,
    surfaceColors: SurfaceColors,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val lessons = course.lessonIds.mapNotNull { id -> collections[id] }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "← All courses",
                    color = accent.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (lessons.isEmpty()) {
            item {
                Text(
                    text = "This course has no resolvable lessons in the bundled catalog.",
                    color = surfaceColors.textMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            items(lessons.size) { index ->
                val lesson = lessons[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceColors.surface)
                        .clickable { onOpenLesson(lesson.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = lesson.title,
                            color = surfaceColors.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lesson.description,
                            color = surfaceColors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${lesson.size} items",
                        color = accent.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    androidx.compose.material3.Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = surfaceColors.textMuted
                    )
                }
            }
        }
    }
}
