import { useState } from 'react';
import WeatherRecommend from '../components/WeatherRecommend';
import ClothingRecommend from '../components/ClothingRecommend';
import './RecommendPage.css';

function RecommendPage() {
  const [activeTab, setActiveTab] = useState('weather'); // weather, personal, ai

  return (
    <div className="recommend-page">
      <header className="recommend-header">
        <h1>추천</h1>
        <p className="recommend-subtitle">날씨에 맞는 옷차림을 추천해드립니다</p>
      </header>

      <div className="recommend-tabs">
        <button
          className={`recommend-tab ${activeTab === 'weather' ? 'active' : ''}`}
          onClick={() => setActiveTab('weather')}
        >
          🌤️ 오늘의 날씨
        </button>
        <button
          className={`recommend-tab ${activeTab === 'personal' ? 'active' : ''}`}
          onClick={() => setActiveTab('personal')}
        >
          ⭐ 나만의 추천
        </button>
        <button
          className={`recommend-tab ${activeTab === 'ai' ? 'active' : ''}`}
          onClick={() => setActiveTab('ai')}
        >
          🤖 AI 추천
        </button>
      </div>

      <div className="recommend-content">
        {activeTab === 'weather' && <WeatherRecommend />}
        {activeTab === 'personal' && <ClothingRecommend />}
        {activeTab === 'ai' && (
          <div className="placeholder-recommend">
            <div className="placeholder-icon">🤖</div>
            <h2>AI 추천 기능</h2>
            <p>AI 기반 맞춤 옷차림 추천 기능은 곧 제공될 예정입니다.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default RecommendPage;
