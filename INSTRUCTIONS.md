# CreamyKeys Standalone (Bilgisayar Klavye Sesi)

Bu uygulama, Minecraft'taki "CreamyKeys" modunun tüm bilgisayarınızda çalışacak şekilde uyarlanmış halidir. Klavyede bir tuşa bastığınızda veya fareye tıkladığınızda mekanik klavye sesi çıkarır.

## Nasıl Kullanılır? (How to Use)

Eğer `.exe` dosyasını oluşturmadıysanız, Python ile şu şekilde çalıştırabilirsiniz:

1.  **Python Yükleyin:** Bilgisayarınızda Python yüklü olmalıdır. [python.org](https://www.python.org/) adresinden indirebilirsiniz. (Yüklerken "Add Python to PATH" seçeneğini işaretlemeyi unutmayın!)
2.  **Gerekli Kütüphaneleri Kurun:** CMD (Komut İstemi) açın ve şu komutu yazıp Enter'a basın:
    ```bash
    pip install pynput pygame
    ```
3.  **Uygulamayı Çalıştırın:** `creamy_keys.py` dosyasına çift tıklayın veya CMD üzerinden şu komutla açın:
    ```bash
    python creamy_keys.py
    ```

## .exe Dosyasına Çevirme (Windows)

Uygulamayı tek bir `.exe` dosyası haline getirip kütüphane kurmadan kullanmak isterseniz:

1.  **PyInstaller Kurun:** CMD'ye şunu yazın:
    ```bash
    pip install pyinstaller
    ```
2.  **EXE Oluşturun:** Proje klasöründe CMD açın ve şu komutu yapıştırın:
    ```bash
    pyinstaller --noconsole --onefile --add-data "sounds;sounds" creamy_keys.py
    ```
    - İşlem bittiğinde `dist` klasörünün içinde `creamy_keys.exe` dosyasını bulabilirsiniz.

## Özellikler

- **Tüm Bilgisayarda Çalışır:** Sadece Minecraft'ta değil, yazı yazdığınız her yerde ses çıkarır.
- **Ses Ayarı:** Uygulama içindeki kaydırıcı (slider) ile ses seviyesini ayarlayabilirsiniz.
- **Farklı Switchler:** Cherry MX Black, Blue, Brown ve Red gibi farklı klavye seslerini seçebilirsiniz.
- **Ayarlar Kaydedilir:** Yaptığınız ses ve paket seçimleri `config.json` dosyasına kaydedilir ve uygulama açıldığında otomatik yüklenir.
