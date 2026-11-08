# Aether HiFi — Hi-Res Audio Player for Android (192 kHz DSP)

Ứng dụng phát và lưu trữ nhạc chất lượng cao (**Hi-Res Audio Audiophile Player**) trên nền tảng Android, trang bị bộ engine xử lý tín hiệu số C++ (**C++ DSP Engine**), hỗ trợ upsampling thời gian thực lên đến **192 kHz / 32-bit Float**, nhập nhạc trực tiếp từ **Google Drive**, tích hợp hệ thống **Equalizer kép (10-Band Graphic EQ & Parametric EQ)** và các tinh chỉnh âm thanh sâu cho từng định dạng nhạc (**FLAC, AAC, WAV, MP3**).

---

## Các tính năng cốt lõi

### 1. Engine Hi-Res Audio Upsampling (Upper Limit 192 kHz)
- **Polyphase Windowed-Sinc Interpolator** viết bằng C++ hiệu năng cao (`app/src/main/cpp/dsp/SincResampler.cpp`).
- Tùy chọn giới hạn tần số lấy mẫu ngõ ra: **44.1 kHz, 48 kHz, 88.2 kHz, 96 kHz, 176.4 kHz, 192 kHz**.
- Hỗ trợ 2 chế độ đáp tuyến pha:
  - **Linear Phase**: Độ trong trẻo tối đa, pha đối xứng, bảo toàn âm hình sân khấu.
  - **Minimum Phase**: Loại bỏ hoàn toàn hiện tượng rung trước (pre-ringing), giữ độ đanh chắc và tự nhiên cho các tiếng gõ trống và nhạc cụ mộc.
- Ngõ ra âm thanh định dạng **32-bit Float PCM** qua Android `AudioTrack` ở chế độ `PERFORMANCE_MODE_LOW_LATENCY`.

### 2. Tinh chỉnh âm thanh chuyên sâu theo từng Codec
- **Đối với AAC & MP3 (Lossy Audio Tuning)**:
  - **DSEE-Style Harmonic Reconstruction**: Thuật toán tái tạo sóng hài bậc chẵn và lẻ ở dải siêu cao (16 kHz – 24 kHz+) từ vùng tần số trung cao, khôi phục lại "độ thoáng" (air), độ chi tiết và độ sáng đã bị thuật toán nén AAC cắt gọt.
  - **Anti-distortion Limiter**: Bộ giới hạn bão hòa mềm (soft-saturation) chống hiện tượng méo tiếng do xung clipping kỹ thuật số.
- **Đối với FLAC & Lossless (Audiophile Tuning)**:
  - **Bit-Perfect Bypass**: Cho phép tín hiệu bit-stream nguyên bản đi thẳng ra DAC rời (USB DAC/Dongle) mà không can thiệp DSP.
  - **Meier Binaural Crossfeed**: Mô phỏng sự giao thoa âm học tự nhiên giữa hai tai như khi nghe loa stereo ngoài phòng, xoá bỏ cảm giác "âm thanh nằm kẹt trong đầu", giảm mỏi tai khi nghe tai nghe trong thời gian dài.
  - **Smooth Roll-Off Anti-Aliasing**: Bộ lọc dốc mượt mà làm dịu nhiễu siêu âm từ các bản thu cổ hoặc bộ giải mã delta-sigma giá rẻ.

### 3. Hệ thống Equalizer Audiophile Đẳng Cấp
- **10-Band Graphic Equalizer**:
  - 10 dải tần chuẩn ISO: `31.25 Hz, 62.5 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz`.
  - Khoảng điều chỉnh ±12 dB kèm bộ tiền khuếch đại (Preamp Gain).
  - Cài đặt sẵn (Presets): *Flat, Audiophile Pure, Bass Boost, Treble Air, Vocal Presence, Acoustic / Jazz, Rock / Hi-Fi, Electronic / EDM*.
- **Parametric Equalizer (PEQ)**:
  - 7 dải lọc độc lập: Peaking, Low Shelf, High Shelf, Band Pass, Notch.
  - Tùy chỉnh tự do: **Tần số (Frequency: 20 Hz - 20,000 Hz)**, **Độ lợi (Gain: ±15 dB)**, **Hệ số Q (Q Factor: 0.1 - 10.0)**.
  - **Biểu đồ đáp tuyến tần số (Frequency Response Curve)** hiển thị trực quan theo thời gian thực trên Canvas.

### 4. Port nhạc trực tiếp từ Google Drive
- **Storage Access Framework (SAF)**: Nhấn chọn file hoặc nguyên thư mục nhạc trên Google Drive thông qua giao diện hệ thống Android. Nhạc được sao chép nguyên gốc (bit-for-bit lossless) về bộ nhớ máy để phát tức thì với độ trễ thấp nhất.
- **Tải qua Link chia sẻ Google Drive**: Dán link file Google Drive (dạng `https://drive.google.com/file/d/.../view`), app tự động phân tích ID, tải file trực tiếp với thanh tiến trình % và gắn thẻ thư viện tự động.

### 5. Giao diện Jetpack Compose & Real-time FFT Visualizer
- Giao diện Dark Theme hiện đại kết hợp màu vàng Champagne Gold và xanh Neon Cyan.
- **Real-time 64-band Spectrum Visualizer**: Phân tích phổ tín hiệu âm thanh ngõ ra qua biến đổi Fourier nhanh (Radix-2 FFT) ở tần số làm tươi ~30 FPS.
- Đĩa vinyl xoay tròn theo nhịp điệu phát nhạc.
- Background Audio Playback Service kèm thanh điều khiển thông báo hệ thống và màn hình khóa.

---

## Cấu trúc thư mục dự án

```
HiresAudioPlayer/
├── .github/workflows/
│   └── build-apk.yml               # GitHub Actions CI tự động build APK
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── cpp/                     # C++ Native DSP Engine
│   │   │   ├── CMakeLists.txt
│   │   │   ├── native-lib.cpp       # JNI Bridge
│   │   │   └── dsp/
│   │   │       ├── AudioPipeline.h/.cpp
│   │   │       ├── BiquadFilter.h/.cpp
│   │   │       ├── CrossfeedFilter.h/.cpp
│   │   │       ├── FftAnalyzer.h/.cpp
│   │   │       ├── GraphicEqualizer.h/.cpp
│   │   │       ├── HarmonicExciter.h/.cpp
│   │   │       ├── ParametricEqualizer.h/.cpp
│   │   │       └── SincResampler.h/.cpp
│   │   ├── java/com/hiresaudioplayer/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── audio/
│   │   │   │   ├── AudioPlaybackService.kt
│   │   │   │   ├── HiResAudioPlayer.kt
│   │   │   │   └── NativeAudioEngine.kt
│   │   │   ├── data/
│   │   │   │   ├── MusicTrack.kt
│   │   │   │   └── TrackRepository.kt
│   │   │   ├── dsp/
│   │   │   │   ├── CodecProfiles.kt
│   │   │   │   ├── DspManager.kt
│   │   │   │   └── EqualizerPreset.kt
│   │   │   ├── gdrive/
│   │   │   │   └── GoogleDriveManager.kt
│   │   │   └── ui/
│   │   │       ├── Navigation.kt
│   │   │       ├── components/SpectrumVisualizer.kt
│   │   │       ├── screens/
│   │   │       │   ├── DspSettingsScreen.kt
│   │   │       │   ├── EqualizerScreen.kt
│   │   │       │   ├── GoogleDriveScreen.kt
│   │   │       │   ├── LibraryScreen.kt
│   │   │       │   └── NowPlayingScreen.kt
│   │   │       └── theme/
│   │   └── res/
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Hướng dẫn Build & Cài đặt APK

### Cách 1: Tải APK tự động qua GitHub Actions (Không cần cài Android Studio)
1. Đẩy (push) mã nguồn dự án này lên tài khoản GitHub của bạn:
   ```bash
   git init
   git add .
   git commit -m "Initial commit for HiRes Audio Player"
   git remote add origin https://github.com/<your-username>/HiresAudioPlayer.git
   git push -u origin main
   ```
2. Mở repository trên trình duyệt GitHub, bấm vào tab **Actions**.
3. Bạn sẽ thấy workflow **"Build Android Hi-Res Player APK"** tự động chạy.
4. Khi chạy xong (khoảng 3-5 phút), bấm vào bản build và tải tệp **`AetherHiFi-Debug-APK`** về máy điện thoại Android để cài đặt ngay.

### Cách 2: Build trực tiếp trên máy bằng Android Studio
1. Cài đặt **Android Studio** (Hedgehog hoặc mới hơn).
2. Mở Android Studio và chọn **Open** -> Chọn thư mục `HiresAudioPlayer`.
3. Đảm bảo đã cài **Android SDK 35** và **NDK (26.1+)** trong `SDK Manager`.
4. Bấm **Build** -> **Build Bundle(s) / APK(s)** -> **Build APK(s)**.
5. File APK xuất ra tại: `app/build/outputs/apk/debug/app-debug.apk`.

