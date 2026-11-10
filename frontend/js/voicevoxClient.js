/**
 * KizunaTutor - VOICEVOX Engine Client & AI Audio Synthesizer
 * Tích hợp giọng đọc tiếng Nhật tự nhiên chuẩn trọng âm (pitch accent)
 * Kết nối: HTTP REST API cục bộ tại http://127.0.0.1:50021 (hoặc qua Proxy /api/voicevox/*)
 * Tự động chuyển đổi Fallback mượt mà sang Web Speech API khi VOICEVOX offline.
 */

// Danh sách diễn họa giọng đọc khuyên dùng
export const VOICEVOX_SPEAKERS = [
  { id: 2, name: 'Shikoku Metan (四国めたん - Rõ ràng, sư phạm)', tag: 'Nữ / Chuẩn' },
  { id: 3, name: 'Zundamon (ずんだもん - Vui nhộn, đáng yêu)', tag: 'Linh vật' },
  { id: 8, name: 'Kasukabe Tsumugi (春日部つむぎ - Tự nhiên, ấm áp)', tag: 'Nữ sinh' },
  { id: 10, name: 'Amehare Hau (雨晴はう - Dịu dàng, y tá)', tag: 'Trầm ấm' },
  { id: 14, name: 'Meimei Himari (冥鳴ひまり - Trong trẻo, thanh tao)', tag: 'Thiếu nữ' }
];

class VoicevoxService {
  constructor() {
    this.primaryUrl = '/api/voicevox';
    this.directUrl = 'http://127.0.0.1:50021';
    this.audioCache = new Map(); // Cache blob URL theo key [text_speaker]
    this.currentAudio = null;
    this.isOnline = null; // null: chưa kiểm tra, true/false
    this.lastCheckTime = 0;
    this.CHECK_INTERVAL = 20000; // 20 giây kiểm tra lại một lần
    this.defaultSpeaker = 2; // Shikoku Metan
    this.hasNotifiedOffline = false;

    // Đọc cài đặt speaker từ localStorage nếu có
    try {
      const saved = localStorage.getItem('kizuna_voicevox_speaker');
      if (saved) this.defaultSpeaker = parseInt(saved, 10) || 2;
    } catch (e) {}
  }

  /**
   * Đặt nhân vật đọc mặc định
   */
  setSpeaker(speakerId) {
    this.defaultSpeaker = speakerId;
    try {
      localStorage.setItem('kizuna_voicevox_speaker', String(speakerId));
    } catch (e) {}
  }

  getSpeaker() {
    return this.defaultSpeaker;
  }

  /**
   * Kiểm tra VOICEVOX Engine có đang hoạt động trên cổng 50021 không
   */
  async checkOnline(force = false) {
    const now = Date.now();
    if (!force && this.isOnline !== null && (now - this.lastCheckTime < this.CHECK_INTERVAL)) {
      return this.isOnline;
    }

    this.lastCheckTime = now;

    // Thử qua proxy nội bộ trước (tránh CORS)
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1200);
      const res = await fetch(`${this.primaryUrl}/version`, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) {
        this.isOnline = true;
        return true;
      }
    } catch (e) {}

    // Thử trực tiếp 127.0.0.1:50021
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 1200);
      const res = await fetch(`${this.directUrl}/version`, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) {
        this.isOnline = true;
        return true;
      }
    } catch (e) {}

    this.isOnline = false;
    return false;
  }

  /**
   * Trích xuất văn bản tiếng Nhật thuần túy từ chuỗi đầu vào (loại bỏ tiếng Việt hoặc format On/Kun)
   */
  cleanJapaneseText(raw) {
    if (!raw) return '';
    let text = String(raw).trim();

    // Nếu có dạng On: ニチ, Kun: ひ / Nghĩa: ...
    if (text.includes('On:') || text.includes('Kun:') || text.includes(' / ')) {
      // Ưu tiên lấy phần Kanji hoặc Kana đầu tiên
      const match = text.match(/[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FAF]+/);
      if (match) return match[0];
    }

    // Nếu chứa ngoặc đơn (Ví dụ: 桜 (さくら)), giữ lại kanji hoặc furigana
    const parenMatch = text.match(/^([^\(（]+)/);
    if (parenMatch && parenMatch[1].trim()) {
      text = parenMatch[1].trim();
    }

    return text;
  }

  /**
   * Phát âm văn bản tiếng Nhật
   * @param {string} rawText Văn bản cần đọc
   * @param {object} options Tùy chọn speaker, speed, pitch
   */
  async speak(rawText, options = {}) {
    const text = this.cleanJapaneseText(rawText);
    if (!text) return false;

    const speaker = options.speaker || this.defaultSpeaker;
    const cacheKey = `${text}_${speaker}`;

    // Dừng âm thanh đang phát trước đó
    this.stop();

    // 1. Kiểm tra Cache bộ nhớ
    if (this.audioCache.has(cacheKey)) {
      const cachedUrl = this.audioCache.get(cacheKey);
      return this.playAudioUrl(cachedUrl);
    }

    // 2. Kiểm tra VOICEVOX engine
    const online = await this.checkOnline();

    if (online) {
      try {
        const audioUrl = await this.synthesizeVoicevox(text, speaker, options);
        if (audioUrl) {
          this.audioCache.set(cacheKey, audioUrl);
          return await this.playAudioUrl(audioUrl);
        }
      } catch (err) {
        console.warn('[VOICEVOX] Lỗi tổng hợp giọng nói, chuyển sang fallback:', err);
      }
    }

    // 3. Fallback sang Web Speech API của trình duyệt nếu VOICEVOX không khả dụng
    return this.fallbackBrowserSpeech(text);
  }

  /**
   * Gọi API VOICEVOX để tổng hợp âm thanh (audio_query -> synthesis)
   */
  async synthesizeVoicevox(text, speaker, options = {}) {
    const baseUrl = this.primaryUrl; // Dùng proxy tránh CORS

    // Bước 1: audio_query
    const queryUrl = `${baseUrl}/audio_query?text=${encodeURIComponent(text)}&speaker=${encodeURIComponent(speaker)}`;
    const queryRes = await fetch(queryUrl, {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    });

    if (!queryRes.ok) {
      throw new Error(`audio_query failed with status ${queryRes.status}`);
    }

    const queryData = await queryRes.json();

    // Tùy chỉnh tốc độ đọc nếu có
    if (options.speedScale) {
      queryData.speedScale = options.speedScale;
    } else {
      queryData.speedScale = 0.95; // Tốc độ hơi chậm nhẹ cho học viên dễ nghe
    }

    // Bước 2: synthesis
    const synthUrl = `${baseUrl}/synthesis?speaker=${encodeURIComponent(speaker)}`;
    const synthRes = await fetch(synthUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'audio/wav'
      },
      body: JSON.stringify(queryData)
    });

    if (!synthRes.ok) {
      throw new Error(`synthesis failed with status ${synthRes.status}`);
    }

    const audioBlob = await synthRes.blob();
    return URL.createObjectURL(audioBlob);
  }

  /**
   * Phát file Audio từ Blob URL
   */
  playAudioUrl(url) {
    return new Promise((resolve, reject) => {
      try {
        this.currentAudio = new Audio(url);
        this.currentAudio.onended = () => {
          this.currentAudio = null;
          resolve(true);
        };
        this.currentAudio.onerror = (e) => {
          this.currentAudio = null;
          resolve(false);
        };
        this.currentAudio.play().catch(err => {
          console.debug('Autoplay hoặc audio error:', err);
          resolve(false);
        });
      } catch (e) {
        resolve(false);
      }
    });
  }

  /**
   * Phát âm thanh qua Web Speech API (Fallback)
   */
  fallbackBrowserSpeech(text) {
    if (!('speechSynthesis' in window)) {
      console.warn('Trình duyệt không hỗ trợ Web Speech API.');
      return false;
    }

    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'ja-JP';
      utterance.rate = 0.88;

      // Tìm giọng tiếng Nhật chuẩn nếu có
      const voices = window.speechSynthesis.getVoices();
      const jaVoice = voices.find(v => v.lang.startsWith('ja') || v.lang.includes('JP'));
      if (jaVoice) {
        utterance.voice = jaVoice;
      }

      window.speechSynthesis.speak(utterance);
      return true;
    } catch (e) {
      console.debug('Lỗi phát âm Web Speech API:', e);
      return false;
    }
  }

  /**
   * Dừng âm thanh đang phát
   */
  stop() {
    if (this.currentAudio) {
      try {
        this.currentAudio.pause();
        this.currentAudio.currentTime = 0;
      } catch (e) {}
      this.currentAudio = null;
    }
    if ('speechSynthesis' in window) {
      try {
        window.speechSynthesis.cancel();
      } catch (e) {}
    }
  }
}

// Khởi tạo instance đơn lẻ (Singleton)
export const voicevoxClient = new VoicevoxService();

// Gắn vào window để bất kỳ trang nào cũng có thể gọi trực tiếp
if (typeof window !== 'undefined') {
  window.voicevoxClient = voicevoxClient;
  window.voicevoxSpeak = (text, options) => voicevoxClient.speak(text, options);
}

export default voicevoxClient;
