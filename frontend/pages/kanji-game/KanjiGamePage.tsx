import React, { useState, useEffect, useRef } from 'react';
import { PageContainer } from '../../components/ui/PageContainer';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Input } from '../../components/ui/Input';

export type GameState = 'LOBBY' | 'PLAYING' | 'GAMEOVER';
export type GameSpeed = 'chill' | 'standard' | 'blitz';

export interface MissedWord {
  char: string;
  hanViet: string;
  romaji: string;
  reading: string;
  meaning: string;
}

export interface KanjiGamePageProps {
  deckId?: string;
  deckTitle?: string;
  cardCount?: number;
}

export const KanjiGamePage: React.FC<KanjiGamePageProps> = ({
  deckId = 'deck_sample',
  deckTitle = 'Bộ Thẻ Cá Nhân',
  cardCount = 20
}) => {
  // State Machine
  const [gameState, setGameState] = useState<GameState>('LOBBY');
  
  // Game Setup Configurations
  const [duration, setDuration] = useState<number>(60); // 60s | 180s | 300s
  const [speed, setSpeed] = useState<GameSpeed>('standard');
  
  // Gameplay In-flight Stats
  const [score, setScore] = useState<number>(0);
  const [hits, setHits] = useState<number>(0);
  const [totalAttempts, setTotalAttempts] = useState<number>(0);
  const [timeLeft, setTimeLeft] = useState<number>(60);
  const [inputVal, setInputVal] = useState<string>('');
  const [showExitModal, setShowExitModal] = useState<boolean>(false);
  
  // Game Over Results
  const [missedWords, setMissedWords] = useState<MissedWord[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  // Auto-focus input when entering gameplay
  useEffect(() => {
    if (gameState === 'PLAYING') {
      inputRef.current?.focus();
    }
  }, [gameState]);

  // Start Game handler
  const handleStartGame = () => {
    setScore(0);
    setHits(0);
    setTotalAttempts(0);
    setTimeLeft(duration);
    setMissedWords([]);
    setInputVal('');
    setGameState('PLAYING');
  };

  const accuracy = totalAttempts > 0 ? Math.round((hits / totalAttempts) * 100) : 100;

  // Format MM:SS
  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  return (
    <PageContainer
      maxWidth="6xl"
      header={{
        title: 'Đấu Trường Kanji Bắn Chữ',
        subtitle: 'Luyện phản xạ đọc Hiragana, Romaji hoặc âm Hán Việt để bắn hạ chữ rơi trước khi chạm vạch đỏ!',
        badgeText: 'Kanji Rain Typing',
        breadcrumbs: [
          { label: 'Trang chủ', href: '/learner/dashboard' },
          { label: 'Bộ thẻ', href: '/flashcards' },
          { label: deckTitle, href: `/learner/deck-detail?id=${deckId}` },
          { label: 'Bắn chữ Kanji' }
        ]
      }}
    >
      {/* ========================================================================= */}
      {/* MÀN HÌNH 1: CÀI ĐẶT TRẬN ĐẤU (SETUP / LOBBY)                               */}
      {/* ========================================================================= */}
      {gameState === 'LOBBY' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
          {/* Card Cấu hình Trận đấu */}
          <div className="lg:col-span-7 space-y-4">
            <Card variant="brutalist" className="space-y-4">
              <div className="flex items-center justify-between border-b border-[#E2E8F0] pb-3">
                <span className="text-sm font-bold text-[#0F172A] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px] text-[#831843]">tune</span>
                  Cấu Hình Trận Đấu
                </span>
                <Badge variant="pink">{deckTitle} • {cardCount} thẻ</Badge>
              </div>

              {/* Lựa chọn Thời lượng */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-[#0F172A] block">
                  Thời lượng thử thách:
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[60, 180, 300].map((sec) => (
                    <button
                      key={sec}
                      type="button"
                      onClick={() => setDuration(sec)}
                      className={`h-9 rounded-lg border font-semibold text-xs transition-all ${
                        duration === sec
                          ? 'bg-[#1E293B] text-white border-[#0F172A] shadow-sm'
                          : 'bg-white text-[#334155] border-[#CBD5E1] hover:bg-slate-50'
                      }`}
                    >
                      {sec} giây
                    </button>
                  ))}
                </div>
              </div>

              {/* Lựa chọn Tốc độ rơi */}
              <div className="space-y-2">
                <label className="text-xs font-bold text-[#0F172A] block">
                  Tốc độ chữ rơi:
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { key: 'chill', label: 'Thư thái' },
                    { key: 'standard', label: 'Tiêu chuẩn' },
                    { key: 'blitz', label: 'Siêu tốc' }
                  ].map((spd) => (
                    <button
                      key={spd.key}
                      type="button"
                      onClick={() => setSpeed(spd.key as GameSpeed)}
                      className={`h-9 rounded-lg border font-semibold text-xs transition-all ${
                        speed === spd.key
                          ? 'bg-[#FFE5EC] text-[#831843] border-[#FBCFE8] shadow-xs'
                          : 'bg-white text-[#334155] border-[#CBD5E1] hover:bg-slate-50'
                      }`}
                    >
                      {spd.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Nút Khởi động Trận đấu */}
              <div className="pt-2">
                <Button
                  variant="retro"
                  size="lg"
                  fullWidth
                  onClick={handleStartGame}
                  className="bg-[#1E293B] text-white hover:bg-[#334155]"
                >
                  <span className="material-symbols-outlined text-[20px]">play_circle</span>
                  Bắt Đầu Chơi Ngay
                </Button>
              </div>
            </Card>

            {/* Mẹo Luyện tập */}
            <Card variant="tinted-pink">
              <div className="flex items-start gap-2.5">
                <span className="material-symbols-outlined text-[#831843] text-lg shrink-0 mt-0.5">verified</span>
                <p className="text-xs text-[#831843] leading-relaxed">
                  <strong>Tự động nhận dạng:</strong> Bạn có thể gõ bằng cách đọc Hiragana (vd: <code>nichi</code> hoặc <code>にち</code>) hoặc trực tiếp âm Hán Việt (vd: <code>NHAT</code>). Hệ thống tự khớp và hạ gục chữ ngay lập tức!
                </p>
              </div>
            </Card>
          </div>

          {/* Bảng Thành Tích (Leaderboard / Điểm Cao) */}
          <div className="lg:col-span-5 space-y-4">
            <Card variant="default" className="space-y-3">
              <div className="flex items-center justify-between border-b border-[#E2E8F0] pb-2">
                <span className="text-xs font-bold text-[#0F172A] uppercase tracking-wide flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[16px] text-amber-500">trophy</span>
                  Kỷ Lục Cá Nhân
                </span>
                <span className="text-[11px] text-[#64748B]">Xếp hạng cao nhất</span>
              </div>

              <div className="divide-y divide-[#E2E8F0] text-xs">
                {[
                  { rank: '#1', score: 1450, acc: '95%', badge: '🥇 Vàng' },
                  { rank: '#2', score: 1120, acc: '88%', badge: '🥈 Bạc' },
                  { rank: '#3', score: 860, acc: '82%', badge: '🥉 Đồng' }
                ].map((item, idx) => (
                  <div key={idx} className="py-2.5 flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-[#0F172A] w-6">{item.rank}</span>
                      <span className="text-xs font-semibold text-[#831843]">{item.badge}</span>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-[#0F172A] font-mono">{item.score} đ</div>
                      <div className="text-[10px] text-[#64748B]">Độ chính xác: {item.acc}</div>
                    </div>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MÀN HÌNH 2: MÀN HÌNH CHƠI GAME (GAMEPLAY CANVAS)                          */}
      {/* ========================================================================= */}
      {gameState === 'PLAYING' && (
        <div className="space-y-3">
          {/* Header Bảng Điều Khiển Trận Đấu */}
          <div className="flex items-center justify-between bg-white border border-[#CBD5E1] rounded-xl px-4 py-2.5 shadow-sm">
            <div className="flex items-center gap-4">
              <div>
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Điểm số</span>
                <span className="text-lg font-extrabold text-[#0F172A] font-mono">{score}</span>
              </div>
              <div className="h-7 w-[1px] bg-slate-200" />
              <div>
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Bắn trúng</span>
                <span className="text-sm font-bold text-[#059669] font-mono">{hits} ({accuracy}%)</span>
              </div>
            </div>

            {/* Đồng hồ đếm ngược */}
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-rose-600 text-[18px]">timer</span>
              <span className="text-xl font-extrabold text-rose-600 font-mono tracking-wider">
                {formatTime(timeLeft)}
              </span>
            </div>

            {/* Nút Bỏ dở / Thoát */}
            <Button
              variant="danger"
              size="sm"
              onClick={() => setShowExitModal(true)}
            >
              Thoát trận
            </Button>
          </div>

          {/* Không gian Canvas Chữ Rơi & Vạch Đỏ Cảnh Báo */}
          <div className="relative w-full h-[450px] bg-[#FAF8F2] border-2 border-[#0F172A] rounded-xl overflow-hidden shadow-[3px_3px_0px_0px_#0F172A]">
            {/* Lưới tọa độ Shodo Canvas */}
            <div className="absolute inset-0 pointer-events-none opacity-30 bg-[linear-gradient(to_right,#E2E8F0_1px,transparent_1px),linear-gradient(to_bottom,#E2E8F0_1px,transparent_1px)] bg-[size:32px_32px]" />

            {/* Chữ Hán đang rơi (Demo Render) */}
            <div className="absolute top-24 left-1/3 text-center animate-bounce">
              <span className="font-japanese text-5xl font-extrabold text-[#0F172A] drop-shadow-sm">
                日
              </span>
              <span className="block text-[11px] font-bold text-[#831843] bg-white/90 border border-[#CBD5E1] rounded px-1 mt-1 shadow-xs">
                NHẬT
              </span>
            </div>

            {/* Vạch Đỏ Deadline Nguy Hiểm */}
            <div className="absolute bottom-4 inset-x-0 border-b-2 border-dashed border-rose-500 flex justify-between px-3 text-[10px] font-bold text-rose-600 uppercase tracking-widest pointer-events-none">
              <span>⚠️ Vạch Cảnh Báo</span>
              <span>Chạm Vạch = Mất Lượt</span>
            </div>
          </div>

          {/* Khu vực Gõ Bắn Phản Xạ */}
          <div className="max-w-md mx-auto">
            <Input
              ref={inputRef}
              autoFocus
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder="Gõ Romaji, Hiragana hoặc Hán Việt để bắn hạ chữ..."
              className="text-center font-bold text-base h-11 border-2 border-[#0F172A] shadow-[2px_2px_0px_0px_#0F172A]"
            />
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MÀN HÌNH 3: BÁO CÁO KẾT QUẢ (GAME OVER / RESULT)                           */}
      {/* ========================================================================= */}
      {gameState === 'GAMEOVER' && (
        <div className="max-w-2xl mx-auto space-y-4">
          <Card variant="brutalist" className="text-center p-6 space-y-4">
            <div className="inline-flex p-3 rounded-full bg-[#FFE5EC] border border-[#FBCFE8] text-[#831843]">
              <span className="material-symbols-outlined text-4xl">emoji_events</span>
            </div>

            <h2 className="text-2xl font-extrabold text-[#0F172A]">Kết Thúc Lượt Chơi!</h2>
            <p className="text-xs text-[#64748B]">Bạn đã hoàn thành lượt bắn chữ Kanji xuất sắc.</p>

            {/* Grid 4 Chỉ số */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-2">
              <div className="p-3 bg-slate-50 border border-[#E2E8F0] rounded-lg">
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Điểm số</span>
                <span className="text-xl font-extrabold text-[#0F172A] font-mono">{score}</span>
              </div>
              <div className="p-3 bg-slate-50 border border-[#E2E8F0] rounded-lg">
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Bắn trúng</span>
                <span className="text-xl font-extrabold text-[#059669] font-mono">{hits}</span>
              </div>
              <div className="p-3 bg-slate-50 border border-[#E2E8F0] rounded-lg">
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Chính xác</span>
                <span className="text-xl font-extrabold text-[#006194] font-mono">{accuracy}%</span>
              </div>
              <div className="p-3 bg-slate-50 border border-[#E2E8F0] rounded-lg">
                <span className="text-[10px] text-[#64748B] block font-semibold uppercase">Bắn trượt</span>
                <span className="text-xl font-extrabold text-rose-600 font-mono">{missedWords.length}</span>
              </div>
            </div>

            {/* Nút Thao Tác */}
            <div className="flex items-center justify-center gap-3 pt-3">
              <Button
                variant="secondary"
                size="md"
                onClick={() => setGameState('LOBBY')}
              >
                Về Cài Đặt
              </Button>
              <Button
                variant="primary"
                size="md"
                onClick={handleStartGame}
              >
                Chơi Lại Ngay
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* Modal Xác nhận Thoát Trận */}
      {showExitModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <Card variant="brutalist" className="max-w-sm w-full p-5 space-y-4 bg-white">
            <h3 className="text-base font-bold text-[#0F172A]">Xác Nhận Dừng Trận Đấu?</h3>
            <p className="text-xs text-[#64748B]">Điểm số hiện tại của lượt chơi này sẽ không được lưu vào kỷ lục.</p>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="secondary" size="sm" onClick={() => setShowExitModal(false)}>
                Tiếp tục chơi
              </Button>
              <Button variant="danger" size="sm" onClick={() => { setShowExitModal(false); setGameState('LOBBY'); }}>
                Rời trận
              </Button>
            </div>
          </Card>
        </div>
      )}
    </PageContainer>
  );
};

export default KanjiGamePage;