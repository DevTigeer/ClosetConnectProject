import { createContext, useContext, useState, useCallback, useEffect } from 'react';

const ClothUploadContext = createContext();

const STORAGE_KEY = 'cloth_active_uploads';

export function ClothUploadProvider({ children }) {
  // localStorage에서 초기값 복구 (PROCESSING 상태만)
  const [activeUploads, setActiveUploads] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        // 진행 중인 작업만 복구 (실패/완료 상태는 제외)
        const processingOnly = parsed.filter(
          upload => upload.status === 'PROCESSING'
        );
        console.log('✅ localStorage에서 진행 중인 작업 복구:', processingOnly);
        if (processingOnly.length < parsed.length) {
          console.log('🗑️  완료/실패 작업 제외:', parsed.length - processingOnly.length, '개');
        }
        return processingOnly;
      }
    } catch (error) {
      console.error('❌ localStorage 복구 실패:', error);
    }
    return [];
  });

  // activeUploads 변경 시 localStorage에 저장
  useEffect(() => {
    try {
      if (activeUploads.length > 0) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(activeUploads));
        console.log('💾 localStorage에 저장:', activeUploads.length, '개 작업');
      } else {
        localStorage.removeItem(STORAGE_KEY);
        console.log('🗑️  localStorage 정리 (작업 없음)');
      }
    } catch (error) {
      console.error('❌ localStorage 저장 실패:', error);
    }
  }, [activeUploads]);

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
