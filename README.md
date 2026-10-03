# DataMeter - Local Network Usage Monitor for Android

DataMeter adalah aplikasi Android production-ready berprinsip **Local-First** untuk memantau penggunaan data internet perangkat (Mobile Data & Wi-Fi) secara detail, akurat, dan aman tanpa transmisi ke cloud.

---

## 📱 Fitur Utama

- **Pemantauan Data Seluler & Wi-Fi**:
  - Statistik unduh (download), unggah (upload), dan total penggunaan secara terpisah dan gabungan.
  - Memanfaatkan API resmi sistem Android: `NetworkStatsManager` dan `ConnectivityManager`.
  - Deteksi status koneksi aktif (Wi-Fi, Seluler, Offline) secara langsung (*real-time*).
- **Periode Waktu Fleksibel**:
  - Hari Ini (intraday breakdown)
  - 7 Hari Terakhir
  - 30 Hari Terakhir
  - Bulan Ini (berdasarkan siklus tagihan kuota)
  - Penyesuaian zona waktu perangkat (tidak mengasumsikan UTC).
- **Riwayat Aktivitas Jaringan (Activity History)**:
  - Pencatatan aktivitas penggunaan data secara kronologis dengan ketelitian waktu hingga detik (`HH:mm:ss`).
  - Interval snapshot efisien (~1 menit) yang hanya mencatat saat terjadi perubahan data (`delta > 0`) tanpa membebani baterai.
  - Rincian per aktivitas: Waktu, Nama Aplikasi, Ikon, Unduh, Unggah, Total, dan Jenis Jaringan (Wi-Fi / Seluler).
  - Filter lengkap: Tanggal (Hari Ini, 7 Hari, 30 Hari, Bulan Ini), Jenis Jaringan (Semua, Wi-Fi, Seluler), Pencarian Aplikasi, dan Pengurutan (Terbaru/Terlama).
  - Bagian "Aktivitas Terakhir" pada Dashboard untuk pemantauan langsung.
  - Penyimpanan database cerdas dengan retensi & agregasi bertingkat (detail per menit < 7 hari, agregasi per jam 7–30 hari, agregasi per hari > 30 hari) agar ukuran database tetap kecil tanpa kehilangan total statistik.
- **Grafik Penggunaan Interaktif**:
  - Grafik batang tersusun (stacked bar) bertenaga Canvas Jetpack Compose.
  - Filter grafik: Semua, Seluler, dan Wi-Fi.
  - Interaksi sentuh (*touch inspection*) untuk melihat rincian tanggal, unduh, dan unggah.
- **Statistik Tingkat Aplikasi (App-Level Usage)**:
  - Daftar aplikasi dengan ikon resmi, nama aplikasi, nama paket, unduh, unggah, dan total.
  - Pencarian aplikasi dan filter jaringan (Semua, Seluler, Wi-Fi).
  - Tampilan rincian aplikasi (*Bottom Sheet*) dan pintasan ke Pengaturan Aplikasi sistem.
  - Penanganan aman (*graceful fallback*) jika perangkat tidak menyediakan statistik tingkat aplikasi.
- **Sistem Batas Kuota (Quota Tracker)**:
  - Batas kuota dalam satuan GB / MB.
  - Siklus reset: Bulanan (dengan pilihan tanggal reset), Mingguan, atau Harian.
  - Indikator progres visual dengan peringatan warna (Aman, Waspada, Bahaya/Habis).
  - Ambang batas peringatan yang dapat dikonfigurasi: 50%, 75%, 90%, dan 100%.
- **Notifikasi Latar Belakang Efisien**:
  - Menggunakan `WorkManager` untuk pengecekan periodik tanpa menguras baterai.
  - Menggunakan `NotificationChannel` resmi dengan penanganan izin Android 13+ (`POST_NOTIFICATIONS`).
  - Pencegahan spam notifikasi dengan pencatatan status ambang batas per siklus di database lokal.
- **Privasi 100% Local-First**:
  - Tidak ada backend / remote server.
  - Tidak ada akun / registrasi / login.
  - Tidak ada iklan (*no ads*).
  - Tidak ada analitik (*no telemetry/trackers*).
  - Aplikasi bahkan tidak meminta izin akses internet (`android.permission.INTERNET`).
- **Personalisasi & Aksesibilitas**:
  - Dukungan tema Gelap (Dark), Terang (Light), dan Default Sistem dengan Material 3.
  - Format data Biner (1024 B = 1 KB) atau Desimal (1000 B = 1 KB).
  - Opsi reset pengaturan lokal dengan dialog konfirmasi.
  - Standar aksesibilitas (ukuran target sentuh >= 48dp, deskripsi konten untuk pembaca layar).

---

## 🏗 Arsitektur & Prinsip Desain

DataMeter dibangun mengikuti prinsip **Clean Architecture** dan **MVVM** (Model-View-ViewModel) dengan Unidirectional Data Flow (UDF):

```text
app/
├── core/
│   ├── network/          # NetworkType, NetworkMonitor, NetworkConnectionState
│   ├── ui/               # UiState, Theme, Color, Typography
│   └── utils/            # DataSizeFormatter, DateUtils, PermissionHelper
│
├── data/
│   ├── local/
│   │   ├── datastore/    # PreferencesManager (DataStore untuk preferensi & kuota)
│   │   └── room/         # Room Database & DAO untuk pencatatan alert kuota
│   ├── networkstats/     # NetworkStatsDataSource & NetworkStatsDataSourceImpl
│   └── repository/       # Implementasi NetworkStatsRepository & PreferencesRepository
│
├── domain/
│   ├── model/            # NetworkUsage, UsageSummary, AppUsage, DailyUsagePoint, QuotaSettings
│   ├── repository/       # Interface NetworkStatsRepository & PreferencesRepository
│   └── usecase/          # Use cases modular untuk agregasi data & logika bisnis
│
├── presentation/
│   ├── apps/             # AppsScreen, AppsViewModel, AppDetailBottomSheet, AppUsageItem
│   ├── components/       # Shared UI (DataMeterTopAppBar, EmptyStateView, PermissionCard)
│   ├── dashboard/        # DashboardScreen, DashboardViewModel, UsageSummaryCard, UsageChartView
│   ├── navigation/       # Screen, NavGraph, Bottom Navigation Bar
│   ├── quota/            # QuotaScreen, QuotaViewModel
│   └── settings/         # SettingsScreen, SettingsViewModel
│
├── worker/               # QuotaCheckWorker (WorkManager) & NotificationHelper
└── di/                   # AppContainer (Dependency Injection modular dan transparan)
```

---

## 🛠 Tech Stack

- **Bahasa**: Kotlin 2.x
- **UI Toolkit**: Jetpack Compose & Material Design 3 (M3)
- **Komponen AndroidX**:
  - `androidx.navigation:navigation-compose`
  - `androidx.lifecycle:lifecycle-viewmodel-compose`
  - `androidx.datastore:datastore-preferences`
  - `androidx.room:room-runtime` & `room-ktx`
  - `androidx.work:work-runtime-ktx`
  - `androidx.core:core-ktx`
- **Konkurensi**: Kotlin Coroutines & Flow / StateFlow
- **API Statistik Jaringan**: `android.app.usage.NetworkStatsManager` & `android.net.ConnectivityManager`
- **Testing**: JUnit 4, Kotlinx Coroutines Test, Robolectric

---

## 🔒 Izin Sistem (Permissions)

| Izin | Kategori | Alasan Penggunaan |
| :--- | :--- | :--- |
| `ACCESS_NETWORK_STATE` | Normal (Install-time) | Memeriksa ketersediaan koneksi internet aktif (Wi-Fi vs Seluler). |
| `PACKAGE_USAGE_STATS` | Khusus (Pengguna) | Diperlukan oleh sistem Android agar `NetworkStatsManager` dapat membaca statistik data. |
| `POST_NOTIFICATIONS` | Runtime (Android 13+) | Mengirimkan notifikasi peringatan kuota jika ambang batas tercapai. |
| `RECEIVE_BOOT_COMPLETED` | Normal (Install-time) | Menjadwalkan ulang pemeriksaan kuota secara hemat daya saat perangkat dinyalakan kembali. |

*Catatan: DataMeter sama sekali tidak meminta izin `android.permission.INTERNET`, sehingga secara teknis mustahil bagi aplikasi untuk mengirimkan data statistik ke pihak luar.*

---

## ⚠️ Keterbatasan Android (Android Limitations)

1. **Akses Penggunaan (Usage Access)**:
   Android mengamankan statistik jaringan di bawah izin `PACKAGE_USAGE_STATS`. Pengguna harus mengaktifkannya secara manual melalui layar Pengaturan Akses Penggunaan. Aplikasi menyediakan panduan dan tombol langsung ke menu pengaturan terkait.
2. **Ketersediaan Statistik Per Aplikasi**:
   Pada beberapa varian OEM Android (atau profil kerja/work profile), sistem dapat membatasi data per UID. DataMeter menangani kondisi ini secara anggun tanpa crash dan menampilkan pesan informatif.
3. **Statistik Seluler Tanpa Privileged Subscriber ID**:
   Mulai Android 10 (API 29), pembacaan `subscriberId` dibatasi. DataMeter menggunakan parameter aman `null` untuk kueri seluler standar yang didukung di semua perangkat modern.

---

## 🚀 Cara Build & Menjalankan Proyek

### Build Secara Lokal

Prasyarat:
- JDK 17 atau yang lebih baru
- Android SDK dengan platform API 36 dan Build-Tools

```bash
# Clone repository
git clone https://github.com/mamang/DataMeter.git
cd DataMeter

# Menjalankan Unit Tests
./gradlew testDebugUnitTest

# Melakukan compile dan build Debug APK
./gradlew assembleDebug
```

Hasil file APK akan berada di: `app/build/outputs/apk/debug/app-debug.apk`.

### Build Menggunakan GitHub Actions

Workflow CI/CD otomatis tersedia di `.github/workflows/android.yml`:
- Menjalankan linting dan unit test pada setiap *push* atau *pull request*.
- Mengompilasi Debug APK dan mengunggahnya sebagai *artifact build*.

---

## 📄 Lisensi

Proyek ini dibuat untuk tujuan keandalan dan privasi pengguna lokal. Bebas digunakan dan dikembangkan untuk penggunaan pribadi maupun produksi.
