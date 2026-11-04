package kr.ac.kw.followme.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kr.ac.kw.followme.data.Destination
import kr.ac.kw.followme.data.sampleDestinations
import kr.ac.kw.followme.ui.components.SecondaryButton
import kr.ac.kw.followme.ui.theme.FollowMeTheme

private const val ALL_CATEGORIES = "전체"

/**
 * 2. 목적지 선택·검색 화면.
 *
 * 검색어와 카테고리 선택은 안내 로직과 무관한 화면 내부 상태라서 GuideState 로 올리지 않고
 * 여기서만 들고 있는다. (화면 전환은 GuideState 만 보고 결정한다)
 */
@Composable
fun SelectingScreen(
    destinations: List<Destination>,
    onSelect: (Destination) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }

    val categories = remember(destinations) {
        listOf(ALL_CATEGORIES) + destinations.map { it.category }.distinct()
    }
    val filtered = remember(destinations, query, category) {
        destinations.filter { dest ->
            (category == ALL_CATEGORIES || dest.category == category) &&
                (query.isBlank() ||
                    dest.displayName.contains(query, ignoreCase = true) ||
                    dest.building.contains(query, ignoreCase = true) ||
                    dest.category.contains(query, ignoreCase = true))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 56.dp, vertical = 36.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "어디로 가시나요?",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SecondaryButton(text = "취소", icon = Icons.Rounded.Close, onClick = onCancel)
        }

        SearchBox(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(top = 28.dp),
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(categories, key = { it }) { item ->
                CategoryChip(
                    label = item,
                    selected = item == category,
                    onClick = { category = item },
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyResult(query)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(filtered, key = { it.id }) { dest ->
                    DestinationCard(dest = dest, onClick = { onSelect(dest) })
                }
            }
        }
    }
}

@Composable
private fun SearchBox(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        textStyle = MaterialTheme.typography.titleLarge,
        placeholder = {
            Text("호실 번호나 이름으로 검색", style = MaterialTheme.typography.titleLarge)
        },
        leadingIcon = {
            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(40.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "검색어 지우기",
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
    )
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else colors.onSurfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(2.dp, colors.outline),
        modifier = Modifier.heightIn(min = 76.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DestinationCard(
    dest: Destination,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(2.dp, colors.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 172.dp)
            .clickable(onClick = onClick),
    ) {
        // 호실명이 카드 폭을 모두 쓰도록 아이콘·분류를 윗줄로 올렸다.
        // (가로 배치로 두면 '모바일로봇 연구실' 같은 이름이 음절 단위로 잘린다)
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(dest.category),
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Text(
                    text = listOf(dest.building, dest.category)
                        .filter { it.isNotBlank() }
                        .joinToString(" \u00b7 "),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
            Text(
                text = dest.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun EmptyResult(query: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(96.dp),
            )
            Text(
                text = if (query.isBlank()) "표시할 목적지가 없습니다" else "'$query' 검색 결과가 없습니다",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(640.dp),
            )
        }
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "강의실" -> Icons.Rounded.MeetingRoom
    "학과 사무실" -> Icons.Rounded.Business
    "연구실" -> Icons.Rounded.Science
    else -> Icons.Rounded.Place
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun SelectingScreenPreview() {
    FollowMeTheme {
        SelectingScreen(destinations = sampleDestinations, onSelect = {}, onCancel = {})
    }
}
