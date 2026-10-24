import sys

content = open("app/src/main/java/com/example/MainActivity.kt").read()

if "FirebaseApp.initializeApp(" not in content:
    content = content.replace("super.onCreate(savedInstanceState)", "super.onCreate(savedInstanceState)\n        \n        try {\n            com.google.firebase.FirebaseApp.initializeApp(this)\n        } catch (e: Exception) {\n            android.util.Log.e(\"FirebaseInit\", \"Firebase initialization failed\", e)\n        }")
    open("app/src/main/java/com/example/MainActivity.kt", "w").write(content)

