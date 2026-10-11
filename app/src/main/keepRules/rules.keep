# R8 keep rules for HeavyDuty. Libraries (Room, Hilt, Coil, Gson, Compose…) ship their own.

# Gson maps JSON keys to field names by reflection. These DTOs read the bundled exercise
# catalog (assets/exercises.json) and read/write backup files, so their field names are a file
# format: renaming or removing them would break seeding and make old backups unreadable.
-keep class com.example.heavyduty.core.data.database.SeedData$ExerciseDto { <init>(...); <fields>; }
-keep class com.example.heavyduty.core.data.repository.*Dto { <init>(...); <fields>; }

# Readable stack traces in crash reports (map them back with the build's mapping.txt).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
