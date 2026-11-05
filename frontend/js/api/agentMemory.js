/**
 * KizunaTutor - Agent Memory Integration Client
 * Microservice Port: 8081 (TencentDB Agent Memory)
 * Contextual learning tracker & Sensei long-term memory
 */

const AGENT_MEMORY_PORT = 8081;
const MEMORY_ENDPOINT = typeof window !== 'undefined' && window.location.port === '3000'
  ? '/memory/append'
  : `http://localhost:${AGENT_MEMORY_PORT}/memory/append`;

export class AgentMemoryService {
  /**
   * Lưu vết nét vẽ hoặc kết quả làm bài vào bộ nhớ dài hạn
   */
  static async recordLearningProgress({ feature, targetChar, score, mistakes, feedback, context }) {
    const memoryRecord = {
      userId: this.getCurrentUserId(),
      timestamp: new Date().toISOString(),
      feature: feature || 'shodo', // 'shodo', 'flashcards', 'grammar', 'ai-writing'
      targetChar: targetChar || null,
      score: score || 0,
      mistakes: mistakes || [],
      feedback: feedback || '',
      context: context || {}
    };

    try {
      const response = await fetch(MEMORY_ENDPOINT, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(memoryRecord),
        signal: AbortSignal.timeout(3000)
      });
      if (response.ok) {
        const json = await response.json();
        return json;
      }
    } catch (err) {
      console.debug('[AgentMemory] Service port 8081 not reached directly, storing in resilient local memory:', err.message);
    }

    // Luôn lưu bản sao cục bộ để Sensei AI giữ liên tục mạch ngữ cảnh
    this.saveLocalMemory(memoryRecord);
    return {
      status: 200,
      success: true,
      message: 'Đã cập nhật trí nhớ Sensei AI (bộ nhớ dự phòng)',
      data: memoryRecord
    };
  }

  /**
   * Phân tích thói quen và lịch sử sai sót để đưa ra lời khuyên cá nhân hóa
   */
  static getPersonalizedSenseiAdvice(feature, targetChar) {
    const list = this.getLocalMemory(feature);
    if (!list || list.length === 0) {
      return 'Sensei đã sẵn sàng đồng hành cùng bạn! Hãy viết thật nắn nót từng nét một nhé.';
    }

    // Tìm lịch sử đối với chữ Hán này
    const charHistory = list.filter(item => item.targetChar === targetChar);
    if (charHistory.length > 1) {
      const lastScore = charHistory[1].score;
      const currentScore = charHistory[0].score;
      if (currentScore > lastScore) {
        return `Sensei nhận thấy bạn đã tiến bộ rõ rệt so với lần tập trước (+${currentScore - lastScore} điểm). Các nét gập đã vững vàng hơn hẳn!`;
      } else {
        return `Lần trước bạn đã đạt ${lastScore} điểm với chữ này. Hãy chú ý giữ trục giữa thật cân và giảm lực đè nét nhé.`;
      }
    }

    // Đánh giá xu hướng chung gần đây
    const averageScore = Math.round(list.slice(0, 5).reduce((acc, it) => acc + (it.score || 0), 0) / Math.min(list.length, 5));
    if (averageScore >= 85) {
      return `Phong độ luyện tập của bạn rất ổn định (trung bình gần đây: ${averageScore} điểm). Hãy duy trì nhịp độ bút này nhé!`;
    } else {
      return `Sensei ghi nhận bạn đang tích cực rèn luyện. Hãy bật "Nét mờ" để căn tỷ lệ các bộ thủ thật đều nhé.`;
    }
  }

  static getCurrentUserId() {
    try {
      const user = JSON.parse(localStorage.getItem('kizuna_user') || '{}');
      return user.id || user.userId || user.email || 'learner-guest';
    } catch (e) {
      return 'learner-guest';
    }
  }

  static saveLocalMemory(record) {
    try {
      const key = `kizuna_memory_${record.feature}`;
      const existing = JSON.parse(localStorage.getItem(key) || '[]');
      existing.unshift(record);
      if (existing.length > 50) existing.pop();
      localStorage.setItem(key, JSON.stringify(existing));
    } catch (e) {}
  }

  static getLocalMemory(feature) {
    try {
      const key = `kizuna_memory_${feature}`;
      return JSON.parse(localStorage.getItem(key) || '[]');
    } catch (e) {
      return [];
    }
  }
}

export default AgentMemoryService;

if (typeof window !== 'undefined') {
  window.AgentMemoryService = AgentMemoryService;
}
