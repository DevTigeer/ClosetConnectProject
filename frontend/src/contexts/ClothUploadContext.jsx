import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { getCurrentUserId } from '../utils/authUtils';

const ClothUploadContext = createContext();

const STORAGE_KEY = 'cloth_active_uploads';
const STORAGE_USER_KEY = 'cloth_active_uploads_userId';
const DISMISSED_KEY = 'cloth_dismissed_uploads'; // 사용자가 삭제한 clothId 목록
const DISMISSED_EXPIRY_MS = 60 * 60 * 1000; // 1시간 후 자동 정리

export function ClothUploadProvider({ children }) {
  // localStorage에서 초기값 복구 (PROCESSING 상태만)
  const [activeUploads, setActiveUploads] = useState(() => {
    try {
      const currentUserId = getCurrentUserId();
      const savedUserId = localStorage.getItem(STORAGE_USER_KEY);

      // 다른 계정의 데이터이면 무시
      if (savedUserId && currentUserId && savedUserId !== String(currentUserId)) {
        console.log('🔄 계정 전환 감지: 이전 activeUploads 정리');
        localStorage.removeItem(STORAGE_KEY);
        localStorage.setItem(STORAGE_USER_KEY, String(currentUserId));
        return [];
      }

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

        // 현재 userId 저장
        if (currentUserId) {
          localStorage.setItem(STORAGE_USER_KEY, String(currentUserId));
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
      const currentUserId = getCurrentUserId();
      if (activeUploads.length > 0) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(activeUploads));
        if (currentUserId) {
          localStorage.setItem(STORAGE_USER_KEY, String(currentUserId));
        }
        console.log('💾 localStorage에 저장:', activeUploads.length, '개 작업');
      } else {
        localStorage.removeItem(STORAGE_KEY);
        localStorage.removeItem(STORAGE_USER_KEY);
        console.log('🗑️  localStorage 정리 (작업 없음)');
      }
    } catch (error) {
      console.error('❌ localStorage 저장 실패:', error);
    }
  }, [activeUploads]);

  // 다중 탭 동기화: storage 이벤트 리스너
  useEffect(() => {
    const handleStorageChange = (e) => {
      // 다른 탭에서 activeUploads 변경 감지
      if (e.key === STORAGE_KEY) {
        console.log('🔄 다른 탭에서 activeUploads 변경 감지');
        try {
          const currentUserId = getCurrentUserId();
          const savedUserId = localStorage.getItem(STORAGE_USER_KEY);

          // 계정이 일치할 때만 동기화
          if (savedUserId && currentUserId && savedUserId === String(currentUserId)) {
            const newValue = e.newValue ? JSON.parse(e.newValue) : [];
            setActiveUploads(newValue);
            console.log('✅ 다른 탭과 동기화 완료:', newValue.length, '개 작업');
          }
        } catch (error) {
          console.error('❌ 탭 간 동기화 실패:', error);
        }
      }

      // Dismissed 항목 동기화
      if (e.key === DISMISSED_KEY) {
        console.log('🔄 다른 탭에서 dismissed 항목 변경 감지');
        // getDismissedItems는 항상 최신 localStorage 값을 읽으므로 별도 처리 불필요
      }
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  // 업로드 추가
  const addUpload = useCallback((clothId, userId) => {
    console.log('➕ addUpload 호출:', { clothId, userId });

    // Dismissed 체크 (사용자가 삭제한 항목은 추가하지 않음)
    if (isDismissed(clothId)) {
      console.log('🚫 Dismissed 항목, 추가 무시:', clothId);
      return;
    }

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
  }, [isDismissed]);

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

  // Dismissed 관리 함수들
  const getDismissedItems = useCallback(() => {
    try {
      const saved = localStorage.getItem(DISMISSED_KEY);
      if (!saved) return {};
      const dismissed = JSON.parse(saved);

      // 오래된 항목 정리 (1시간 경과)
      const now = Date.now();
      const cleaned = Object.entries(dismissed).reduce((acc, [id, timestamp]) => {
        if (now - timestamp < DISMISSED_EXPIRY_MS) {
          acc[id] = timestamp;
        }
        return acc;
      }, {});

      // 정리된 목록 저장
      if (Object.keys(cleaned).length !== Object.keys(dismissed).length) {
        localStorage.setItem(DISMISSED_KEY, JSON.stringify(cleaned));
        console.log('🗑️  오래된 dismissed 항목 정리:', Object.keys(dismissed).length - Object.keys(cleaned).length, '개');
      }

      return cleaned;
    } catch (error) {
      console.error('❌ getDismissedItems 실패:', error);
      return {};
    }
  }, []);

  const isDismissed = useCallback((clothId) => {
    const dismissed = getDismissedItems();
    return clothId in dismissed;
  }, [getDismissedItems]);

  const markDismissed = useCallback((clothId) => {
    try {
      const dismissed = getDismissedItems();
      dismissed[clothId] = Date.now();
      localStorage.setItem(DISMISSED_KEY, JSON.stringify(dismissed));
      console.log('🚫 clothId를 dismissed로 표시:', clothId);
    } catch (error) {
      console.error('❌ markDismissed 실패:', error);
    }
  }, [getDismissedItems]);

  // 업로드 제거 (완료 또는 실패)
  const removeUpload = useCallback((clothId, markAsDismissed = false) => {
    setActiveUploads(prev => prev.filter(upload => upload.clothId !== clothId));

    // markAsDismissed=true면 재추가 방지
    if (markAsDismissed) {
      markDismissed(clothId);
    }
  }, [markDismissed]);

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
    completeUpload,
    isDismissed
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
