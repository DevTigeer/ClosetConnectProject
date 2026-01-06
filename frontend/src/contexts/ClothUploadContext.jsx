import { createContext, useContext, useState, useCallback } from 'react';

const ClothUploadContext = createContext();

export function ClothUploadProvider({ children }) {
  const [activeUploads, setActiveUploads] = useState([]);

  // 업로드 추가
  const addUpload = useCallback((clothId, userId) => {
    console.log('➕ addUpload 호출:', { clothId, userId });
    setActiveUploads(prev => {
      // 중복 체크
      if (prev.some(upload => upload.clothId === clothId)) {
        console.log('⚠️  중복 업로드, 무시:', clothId);
        return prev;
      }
      const newUpload = {
        clothId,
        userId,
        status: 'PROCESSING',
        currentStep: 'AI 처리 시작...',
        progressPercentage: 0,
        timestamp: Date.now()
      };
      console.log('✅ 새 업로드 추가:', newUpload);
      return [...prev, newUpload];
    });
  }, []);

  // 진행도 업데이트
  const updateProgress = useCallback((clothId, progressData) => {
    console.log('🔄 updateProgress 호출:', { clothId, progressData });
    setActiveUploads(prev => {
      const found = prev.find(upload => upload.clothId === clothId);
      if (!found) {
        console.warn('⚠️  업로드를 찾을 수 없음:', clothId, '현재 목록:', prev.map(u => u.clothId));
      }
      const updated = prev.map(upload =>
        upload.clothId === clothId
          ? { ...upload, ...progressData }
          : upload
      );
      console.log('✅ 업데이트 완료, 새 상태:', updated);
      return updated;
    });
  }, []);

  // 업로드 제거 (완료 또는 실패)
  const removeUpload = useCallback((clothId) => {
    setActiveUploads(prev => prev.filter(upload => upload.clothId !== clothId));
  }, []);

  // 완료 처리
  const completeUpload = useCallback((clothId) => {
    updateProgress(clothId, {
      status: 'READY_FOR_REVIEW',
      currentStep: '처리 완료',
      progressPercentage: 100
    });

    // 자동 제거 제거: 사용자가 이미지를 선택한 후에만 제거
    // (ImageSelectionModal에서 onConfirm 시 removeUpload 호출)
  }, [updateProgress]);

  const value = {
    activeUploads,
    addUpload,
    updateProgress,
    removeUpload,
    completeUpload
  };

  return (
    <ClothUploadContext.Provider value={value}>
      {children}
    </ClothUploadContext.Provider>
  );
}

export function useClothUpload() {
  const context = useContext(ClothUploadContext);
  if (!context) {
    throw new Error('useClothUpload must be used within ClothUploadProvider');
  }
  return context;
}
