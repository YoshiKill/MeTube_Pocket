@Composable
fun DownloadItemRow(item: DownloadItem, viewModel: MainViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable { menuExpanded = true }
        .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title ?: item.url ?: "Неизвестно", 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Статус: ${item.status ?: "pending"}", fontSize = 12.sp)
                if (item.status == "downloading") {
                    Text(text = formatSpeed(item.speed), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (item.status == "downloading" && item.percent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = (item.percent / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f).height(6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${"%.1f".format(item.percent)}%", fontSize = 12.sp)
                }
            }

            if (!item.error.isNullOrEmpty()) {
                Text(text = item.error!!, color = Color.Red, fontSize = 11.sp, maxLines = 2)
            }
        }

        // Всплывающее контекстное меню с опциями
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            when (item.status) {
                "downloading", "pending" -> {
                    DropdownMenuItem(
                        text = { Text("Отменить загрузку") },
                        onClick = {
                            item.id?.let { viewModel.cancelDownload(it) }
                            menuExpanded = false
                        }
                    )
                }
                "error" -> {
                    DropdownMenuItem(
                        text = { Text("Повторить загрузку (Retry)") },
                        onClick = {
                            item.id?.let { viewModel.retryDownload(it) }
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить из списка") },
                        onClick = {
                            item.id?.let { viewModel.deleteFromHistory(it) }
                            menuExpanded = false
                        }
                    )
                }
                else -> { // finished / done
                    DropdownMenuItem(
                        text = { Text("Удалить из списка") },
                        onClick = {
                            item.id?.let { viewModel.deleteFromHistory(it) }
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить вместе с файлом") },
                        onClick = {
                            item.id?.let { viewModel.deleteWithFile(it) }
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
