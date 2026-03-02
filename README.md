# Vertical Bottom Nav 🚀

Una librería ligera y personalizable para implementar una navegación inferior vertical en aplicaciones Android.

## 📥 Instalación

### 1. Agregar JitPack a tu proyecto
En tu archivo `settings.gradle.kts` (o `build.gradle` de nivel de proyecto):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}
2. Pegar el contenido
Copiá y pegá este bloque de código tal cual en el editor de GitHub:

Markdown
# Vertical Bottom Nav 🚀

Una librería ligera y personalizable para implementar una navegación inferior vertical en aplicaciones Android.

## 📥 Instalación

### 1. Agregar JitPack a tu proyecto
En tu archivo `settings.gradle.kts` (o `build.gradle` de nivel de proyecto):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}
2. Agregar la dependencia
En el build.gradle.kts de tu módulo app:

Kotlin
dependencies {
    implementation("com.github.cristianramirezarias:vertical-bottom-nav:1.0.0")
}
🛠️ Uso
Para usar el componente en tu layout XML:

XML
<com.krystalshardbrCristianR.vertical_bottom_nav.CristianVerticalNav
    android:id="@+id/verticalNav"
    android:layout_width="wrap_content"
    android:layout_height="match_parent" />
👨‍💻 Autor
Desarrollado por Cristian.
