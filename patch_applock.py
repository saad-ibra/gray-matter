import re

with open('androidApp/src/main/java/com/example/graymatter/android/ui/screens/BiometricLockScreen.kt', 'r') as f:
    content = f.read()

# Make the unlock button sleek
old_button = """            // Unlock button
            Button(
                onClick = onAuthenticate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrayMatterTheme.colors.primary
                )
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }"""

new_button = """            // Sleek Unlock button
            OutlinedButton(
                onClick = onAuthenticate,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GrayMatterTheme.colors.primary,
                    containerColor = GrayMatterTheme.colors.primary.copy(alpha = 0.05f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    GrayMatterTheme.colors.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Tap to Unlock",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }"""

content = content.replace(old_button, new_button)

with open('androidApp/src/main/java/com/example/graymatter/android/ui/screens/BiometricLockScreen.kt', 'w') as f:
    f.write(content)
