CatchUp
=======

This is a fork of hzsweers/CatchUp for the purpose of measuring and improving Kotlin build performance. This was forked from git revision 66adb94 of the original project. The Kotlin version was then updated to 1.3.0-rc-146, Android Gradle plugin updated to 3.3.0-alpha16, and I made miscellaneous hacks to make the project buildable. The point of this is to compare build performance of this project with git revision 48a418d when it was purely Java.

## How to use
- Install the [Gradle Profiler](https://github.com/gradle/gradle-profiler).
- `git clone <project_path>`
- `gradle-profiler --benchmark --project-dir . --scenario-file performance.scenarios incremental_build --output-dir ./profile-out-incremental_build`
