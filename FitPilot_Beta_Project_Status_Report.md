---
title: "FitPilot Beta"
subtitle: "Proje Durumu & Geliştirme Raporu"
author: "Antigravity AI"
date: "Ekim 2026"
---

# 1. Kapak Sayfası (Cover Page)
**Proje Adı:** FitPilot Beta (Önceki Adı: GymFlow AI)
**Rapor Türü:** Proje Durumu & Geliştirme Raporu
**Mevcut Sürüm:** `0.1.0-beta`
**Mevcut Geliştirme Aşaması:** `Phase 11A.14` *(Planlanan ve üzerinde çalışılan aşama)*
**Tarih:** Ekim 2026
**Teknoloji:** Android / Kotlin / Jetpack Compose

---

# 2. Yönetici Özeti (Executive Summary)
**FitPilot Beta**, kullanıcıların fitness yolculuklarını optimize etmeyi amaçlayan, akıllı bir egzersiz koçluğu ve takip uygulamasıdır. Temel felsefesi, karmaşık ve zaman alıcı veri girişlerini basitleştirerek kullanıcının antrenman ve beslenme süreçlerine odaklanmasını sağlamaktır. 

**Mevcut Olgunluk Seviyesi:** Uygulama şu anda *beta* aşamasındadır. Çekirdek antrenman takip sistemi, kronometre mekaniği, beslenme takibi (temel düzeyde) ve Akıllı Koçluk (Smart Coaching) özellikleri uygulanmış olup geliştirme süreci devam etmektedir. Yapay Zeka (AI / Gemini), uygulamanın temel kimliği değil, destekleyici bir katmandır. Phase 11B öncesi, veritabanı karakter kodlaması (mojibake) sorunları ve Smart Recommendation ekranındaki çökmeler (`REST_OR_WALK` senaryosu) gibi kritik iyileştirmelere (Phase 11A.14) odaklanılmaktadır.

---

# 3. Ürün Vizyonu (Product Vision)
FitPilot Beta'nın hedefi, akıllı ve kişiselleştirilmiş bir fitness asistanı olmaktır. 

**Mevcut İşlevsellik (Implemented):**
- Antrenman takibi ve ilerleme (Progression).
- Dinlenme zamanlayıcısı (Rest Timer).
- Basit beslenme (Nutrition) takibi.
- Deterministik Akıllı Koçluk (Readiness Check-in).
- Egzersiz ve set bazlı not sistemi.

**Gelecek Yol Haritası (Future Roadmap):**
- Detaylı ve esnek program oluşturucu (Program Builder).
- Kapsamlı egzersiz kütüphanesi.
- Gelişmiş motivasyon ve analitik özellikleri.

---

# 4. Mevcut Teknoloji Yığını (Current Technology Stack)
- **Kotlin:** Uygulamanın modern ve güvenli bir şekilde geliştirilmesi için birincil dil.
- **Jetpack Compose:** Deklaratif UI (Material 3) bileşenleri ile esnek ve hızlı kullanıcı arayüzü tasarımı.
- **Room:** Çevrimdışı (offline-first) yerel veritabanı persistansı için. Mevcut yapı stabil çalışmaktadır.
- **DataStore:** Kullanıcı tercihleri ve ayarları (örn. Tema, Kronometre Sesi) asenkron olarak saklamak için.
- **Hilt (Dagger):** Bağımlılık enjeksiyonu (Dependency Injection) için.
- **MVVM & Clean Architecture:** Domain (UseCase), Data (Repository/DAO) ve UI (ViewModel) ayrımını sağlamak için.
- **Gemini AI Entegrasyonu:** Beslenme analizi (Food Parsing) ve isteğe bağlı akıllı özellikler için destekleyici servis katmanı.

---

# 5. Mimari Genel Bakış (Architecture Overview)

Uygulama, klasik bir "Clean Architecture" ve MVVM prensiplerine göre tasarlanmıştır:

```text
[ UI / Jetpack Compose ]  -> Kullanıcı arayüzü ve state render
          ↓
[ ViewModels ]            -> UI State yönetimi ve UseCase tetiklemeleri
          ↓
[ UseCases (Domain) ]     -> İş mantığı (Progression, Smart Coaching vb.)
          ↓
[ Repositories ]          -> Veri orkestrasyonu (Data, Domain'e soyutlanır)
          ↓
[ Room DAO / DataStore ]  -> Yerel veritabanı ve ayarlar (Offline-first)
```

**Not:** Deterministik "Smart Coaching", `CalculateSmartRecommendationUseCase` içerisinde yönetilir. Gemini-AI ise sadece `GeminiAiService` aracılığıyla, özellik aktif edildiğinde devreye girmektedir.

---

# 6. Mevcut Uygulama Ekranları (Current Application Screens)

| Ekran Adı | Durum | Açıklama |
| :--- | :--- | :--- |
| **Dashboard** | Uygulanmış (Implemented) | Ana merkez; güncel antrenmanlar ve beslenme özetleri. |
| **Smart Settings** | Uygulanmış (Implemented) | Dil, tema ve Gemini ayarları. |
| **Readiness Check-in** | Uygulanmış (Implemented) | Antrenman öncesi fiziksel durum anketi. |
| **Smart Recommendation** | Uygulanmış (Implemented) | Anket sonucuna göre NORMAL, CONTROLLED, LIGHT, REST_OR_WALK kararı. |
| **Workout Preview** | Uygulanmış (Implemented) | Antrenman başlamadan önce set, ağırlık ve geçmiş notların önizlemesi. |
| **Active Workout** | Uygulanmış (Implemented) | Çekirdek antrenman takip ekranı. Isınma, çalışma setleri, süpersetler. |
| **Workout History** | Uygulanmış (Implemented) | Tamamlanan antrenmanların geçmiş listesi. |
| **Workout Detail** | Uygulanmış (Implemented) | Geçmiş antrenmana ait süre, hacim, RPE ve notların detayı. |
| **Exercise History** | Uygulanmış (Implemented) | Spesifik bir egzersizin geçmiş performans verileri. |
| **Nutrition Dashboard** | Uygulanmış (Implemented) | Günlük kalori ve makro besin özeti. |
| **Add Food / Saved Foods** | Uygulanmış (Implemented) | Manuel besin ekleme ve kaydedilmiş besinleri listeleme. |
| **Body Metrics** | Kısmen Uygulanmış | Kilo ve yağ oranı takibi için arayüz. |
| **Advanced Analytics** | İleriye Ertelendi (Deferred) | Gelişmiş haftalık/aylık grafikler (Şimdilik Analytics Dashboard mevcut). |

---

# 7. Çekirdek Antrenman Sistemi (Core Workout System)
- **Tohumlanmış (Seeded) Programlar:** A (Upper Body 1), B (Lower Body 1), C (Upper Body 2), D (Lower Body 2).
- **Isınma & Çalışma Setleri:** Isınma setleri ve çalışma setleri açıkça ayırt edilmiştir.
- **Etkileşimler:** Kullanıcılar ağırlık, tekrar ve RPE girebilir; seti tamamlayabilir (Complete) veya atlayabilir (Skip).
- **Kardiyo:** Antrenman sonu (Post-Workout) kardiyo takibi mevcuttur.

---

# 8. Dinlenme Zamanlayıcısı (Rest Timer System)
- **Mekanik:** Set tamamlandığında geri sayım başlar. `+30 sn`, `-15 sn` ve `Skip` özellikleri çalışmaktadır.
- **Uyarılar:** 30 saniye kala ve süre bitiminde sesli uyarı verilir. Ses stilleri (TimerSoundStyle) DataStore üzerinden seçilebilir.
- **Test Edilebilirlik:** Sesli uyarılar emülatörde çalışmaktadır. Fiziksel cihazda titreşim özelliği koddadır (Vibrator) ancak emülatör sınırlamaları nedeniyle *sadece* kod seviyesinde doğrulanmıştır.

---

# 9. Notlar Sistemi (Notes System)
- **Hiyerarşi:** Antrenman bazlı, Egzersiz bazlı ve Set bazlı notlar.
- **Kullanım:** Kullanıcılar `Active Workout` sırasında veya önizleme ekranlarında not girebilir. Notlar çevrilmez veya lokalize edilmez; girildiği şekliyle saklanır.
- **Kalıcılık (Persistence):** Room `SetLogEntity` ve `WorkoutSessionEntity` içerisinde tutulur.

---

# 10. Antrenman Geçmişi ve Analitik (History & Analytics)
- **Mevcut Durum:** `Workout History` ekranında güncel süre, hacim, set sayısı özeti sunulmaktadır. İlgili karta tıklanarak `Workout Detail` ekranına geçilebilir.
- **Gelecek Analitik (Analytics Hub):** Haftalık, aylık trendler ve kas dağılımı gibi gelişmiş özellikler yol haritasındadır (Phase 11B ve sonrası).

---

# 11. Beslenme Sistemi (Nutrition System)
- **Manuel Kayıt:** Öğün tipi, kalori, protein, karbonhidrat, yağ ve porsiyon boyutu kaydedilebilir.
- **Yapay Zeka Destekli Analiz:** `AiFoodReviewScreen` üzerinden Gemini destekli serbest metin besin ekleme özelliği entegredir. (Çalışması için Gemini API Key gereklidir).

---

# 12. Vücut Metrikleri (Body Metrics)
Desteklenen temel metrikler "Kilo" ve "Yağ Oranı" (Body Fat). Kullanıcılar manuel veri girişi yapabilir, ancak grafiksel raporlama şimdilik temel düzeydedir.

---

# 13. Akıllı Koçluk Sistemi (Smart Coaching System)
Bu sistem tamamen **deterministiktir** ve AI içermez.
- **NORMAL:** Hedeflenen antrenman.
- **CONTROLLED:** Set sayısı ve RPE hafif düşürülür.
- **LIGHT:** Çok daha düşük hacim ve efor (Aktif toparlanma).
- **REST_OR_WALK:** Antrenmanı pas geçip sadece yürüyüş veya dinlenme kaydetme. (Not: Bu özellikteki UI çökme sorunu Phase 11A.14'te giderilmiştir).

---

# 14. Gemini / Yapay Zeka Özellikleri (AI Features)
- **Kullanım:** Sadece beslenme metinlerini (Food Parsing) çözümlemek ve opsiyonel antrenman özetleri sunmak için kullanılır.
- **Bağımsızlık:** Gemini kapalı olsa dahi uygulama %100 yerel işlevselliğiyle (offline-first) çalışmaya devam eder. API Anahtarı, kullanıcı cihazında DataStore üzerinden lokal tutulur.

---

# 15. Yerelleştirme (Localization)
- **Diller:** İngilizce (Varsayılan) ve Türkçe (Genişletilmiş destek).
- **Temalar:** Açık (Light), Koyu (Dark), Sistem.
- **Durum:** Tarih ve numara formatları `FormatUtils.kt` ve `LocalizationUtils.kt` ile dinamik olarak yerelleştirilmiştir. UTF-8 kodlama sorunu (Mojibake) Room veritabanı tohumlamasından kaynaklanmış olup `MojibakeMigrationUtils.kt` ile onarılmıştır. Yerelleştirme işlemleri mevcut sürümde devam eden (Partially implemented) bir süreçtir.

---

# 16. Veri Modeli (Data Model / Entities)
Temel tablolar:
- `WorkoutTemplateEntity`, `ExerciseEntity` (Şablonlar ve Egzersizler)
- `WorkoutSessionEntity` (Oturum kaydı)
- `WorkoutExerciseSessionEntity` (Oturum içi egzersiz detayı)
- `SetLogEntity` (Kayıtlı setler ve notlar)
- `NutritionEntities` (Günlük kalori/makro)

*(Tüm veri yapısı ilişkisel -Foreign Key- kurallarına uygun olarak Room üzerinde tasarlanmıştır).*

---

# 17. Veritabanı ve Stabilite Tarihçesi (Database Evolution & Stability History)
- **Phase 11A.1 - 11A.3:** Foreign Key çakışmaları ve Kardiyo ekranı sonsuz yüklenme sorunları çözüldü.
- **Phase 11A.5 - 11A.6:** Titreşim izni kilitlenmeleri (crash) ve not görünürlüğü hataları onarıldı.
- **Phase 11A.7 - 11A.10:** Tema (Light/Dark) desteği ve zamanlayıcı stabilizasyonu sağlandı.
- **Phase 11A.11 - 11A.13:** UTF-8 Türkçe karakter bozulmaları (mojibake) paketleme seviyesinde giderildi.
- **Phase 11A.14 (Mevcut Durum):** Smart Recommendation `REST_OR_WALK` rotasyon çökmesi (crash) ve eski (legacy) Room kayıtlarındaki bozuk metinlerin tek seferlik onarımı sağlandı.

---

# 18. QA ve Test Geçmişi (QA / Testing History)
- Testler genel olarak **Windows geliştirme ortamında ve Android Studio Emülatörü** üzerinde yürütülmektedir.
- Uygulama, `EncodingValidationTest` ve `GetProgramRiskRemindersUseCaseTest` gibi Unit Testleri içermektedir.
- Fiziksel cihaz doğrulaması kısıtlıdır; özellikle *Titreşim (Vibration)* davranışı kod bazlı varsayımlara dayanmaktadır.

---

# 19. Bilinen Mevcut Sorunlar (Current Known Issues)

| Etki Seviyesi | Sorun (Issue) | Durum (Status) | Sonraki Adım |
| :--- | :--- | :--- | :--- |
| Kritik (Blocker)| `REST_OR_WALK` modunda çökme (Crash) | Çözüldü (Phase 11A.14) | Emülatörde QA Doğrulaması |
| Yüksek | Tohumlanmış (Seeded) veri Mojibake karakterleri | Çözüldü (Phase 11A.14) | Emülatörde QA Doğrulaması |
| Orta | UI dili değişirken yaşanan siyah parlama (Black flash) | Kısmen Çözüldü | Activity recreation onarımı |
| Düşük | Uygulamanın kalan bazı çevirileri | Devam Ediyor | Kapsamlı string review |

---

# 20. UX / Tasarım Değerlendirmesi (Current UX / Design Assessment)
FitPilot Beta, Material 3 standartlarını başarılı bir şekilde uygulamaktadır. Active Workout ekranı, antrenman sırasındaki etkileşimleri hızlandırmak üzere tasarlanmıştır. Ancak, bazı bileşenler (özellikle veri grafikleri ve analitik paneli) hala erken dönem prototip (early prototype) hissini korumaktadır. Açık/Koyu tema desteği tamamlanmıştır ve okunabilirlik tatmin edicidir.

---

# 21. Marka ve Ürün Kimliği (Brand & Product Identity)
**Önceki Adı:** GymFlow AI
**Yeni Adı:** FitPilot Beta
Karar: Ürün doğrudan bir "Yapay Zeka Chatbotu" gibi konumlandırılmak yerine, sağlam ve güvenilir bir fitness aracı olarak tasarlanmıştır. Gelecekteki marka kimliği de hareket, ilerleme ve stabiliteyi yansıtmalıdır.

---

# 22. Yol Haritası (Roadmap)
**Mevcut Aşama:** Phase 11A.14 (Blocker Hotfix)
**Phase 11B Beklentileri:** 
- **Serbest Antrenman (Free Workout) ve Program Builder:** Kullanıcıların tamamen boş bir şablondan kendi antrenmanlarını oluşturabilmeleri.
- **Egzersiz Kütüphanesi:** Kas grubuna göre filtreleme, alternatif egzersizler.
- İleri Seviye (Future): Hesap ve bulut senkronizasyonu, gelişmiş başarımlar (streaks).

---

# 23. Önerilen Mimari Yaklaşım (Recommended Product Architecture)
- Room ve Offline-first yaklaşımı kesinlikle korunmalıdır.
- Smart Coaching mantığı deterministik (AI'dan bağımsız) kalmaya devam etmelidir.
- Uygulama, gereksiz UI veya kütüphane "rewrite" döngülerinden kaçınarak, mevcut Clean Architecture üzerine inşa edilmeye devam etmelidir.

---

# 24. Ek - Proje Dosya Haritası (Appendix — Project Module Map)
Uygulamanın mevcut kök modülleri:
- `app/src/main/java/com/doruk/fitprogress/`
  - `MainActivity.kt` & `GymFlowApplication.kt`
  - `core/database/` (GymFlowDatabase, DatabaseSeeder, MojibakeMigrationUtils)
  - `core/domain/` (Progression, Coaching ve AI UseCaseleri)
  - `feature/activeworkout/` (ActiveWorkoutScreen, vb.)
  - `feature/history/` (WorkoutHistoryScreen)
  - `feature/coaching/` (CoachingScreen)
  - `theme/` (Theme, Color, Type)

---

# 25. Son Değerlendirme (Final Executive Assessment)

**Neler İyi Çalışıyor?**
Offline-first mimari, lokal veritabanı, çekirdek ağırlık antrenmanı etkileşimi, dinlenme zamanlayıcısı ve deterministik Akıllı Koçluk.

**Nelerin Düzeltilmesi Gerekiyor?**
Son (Phase 11A.14) ile çözülen UTF-8 kodlama problemleri ve geçişteki crash sorunlarının son cihaz kontrolleri.

**Phase 11B İçin Engel Nedir?**
Phase 11A.14 QA testlerinin nihai onayı beklenmektedir. Uygulama henüz Play Store standartlarında "production-ready" değildir ancak temel ürün kimliğini yansıtan stabil bir Beta adayıdır.
