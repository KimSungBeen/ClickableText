# ClickableText

Composable is useful when you want to add a button action to a specific part of continuous text!

# How to

To get a Git project into your build:

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```gradle
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}
```

**Step 2.** Add the dependency

```gradle
dependencies {
    implementation 'com.github.KimSungBeen:ClickableText:Tag'
}
```

# For example

<img width=350 src="https://github.com/user-attachments/assets/f4c8602b-187c-4efe-9ad6-fc22f296bf40">

## The code below is the code used in the test.

```kotlin
@Preview(showBackground = true)
@Composable
private fun TextButtonPreview() {
    var status by remember { mutableStateOf(ClickableText.Status.Default) }

    val buttons: List<ClickableText.Button> = listOf(
        ClickableText.Button(
            indexRange = 0..5,
            onClick = { status = ClickableText.Status.Default },
            trailing = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            }
        ),
        ClickableText.Button(
            indexRange = 17..21,
            onClick = { status = ClickableText.Status.Disabled },
            leading = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            }
        ),
        ClickableText.Button(
            indexRange = 30..33,
            textStyle = TextStyle(
                fontSize = 28.sp,
                color = Color.Blue
            ),
            onClick = {},
            leading = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            },
            trailing = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null
                )
            },
            statusProvider = { status }
        )
    )

    ClickableText(
        fullText = "Hello, I am 000. Nice to meet you :)",
        defaultTextStyle = TextStyle(
            color = Color.Black,
            fontSize = 18.sp
        ),
        buttons = buttons,
        onClick = { clickedButtonIndex: Int ->
            buttons[clickedButtonIndex].onClick()
        }
    )
}
```
