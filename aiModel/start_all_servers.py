#!/usr/bin/env python3
"""
AI Model Servers Launcher
모든 AI 서버를 한 번에 실행하고 실시간 로그를 표시합니다.
"""

import subprocess
import threading
import signal
import sys
import time
import os
from pathlib import Path
from datetime import datetime


class Colors:
    """터미널 색상 코드"""
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    MAGENTA = '\033[95m'
    CYAN = '\033[96m'
    WHITE = '\033[97m'
    RESET = '\033[0m'
    BOLD = '\033[1m'


# 전역 프로세스 리스트
processes = []
threads = []


def print_log(server_name, message, color):
    """
    서버 로그를 색상과 함께 출력

    Args:
        server_name: 서버 이름
        message: 로그 메시지
        color: 터미널 색상 코드
    """
    timestamp = datetime.now().strftime('%H:%M:%S')
    prefix = f"{color}[{timestamp}] [{server_name}]{Colors.RESET}"
    print(f"{prefix} {message.rstrip()}")


def stream_output(process, server_name, color, log_file_path):
    """
    프로세스의 출력을 실시간으로 읽어서 화면과 파일에 출력

    Args:
        process: subprocess.Popen 객체
        server_name: 서버 이름
        color: 터미널 색상 코드
        log_file_path: 로그 파일 경로
    """
    log_file = open(log_file_path, 'w', encoding='utf-8')

    try:
        while True:
            line = process.stdout.readline()
            if not line and process.poll() is not None:
                # 프로세스 종료됨
                break
            if line:
                decoded_line = line.decode('utf-8', errors='replace')
                # 화면에 출력
                print_log(server_name, decoded_line, color)
                # 파일에도 저장
                log_file.write(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {decoded_line}")
                log_file.flush()
    except Exception as e:
        print_log(server_name, f"로그 스트림 에러: {e}", Colors.RED)
    finally:
        log_file.close()
        # 프로세스가 에러로 종료된 경우 추가 정보 출력
        if process.returncode and process.returncode != 0:
            print_log(server_name, f"❌ 서버 종료 (Exit Code: {process.returncode})", Colors.RED)


def start_server(server_config):
    """
    서버를 시작하고 로그 스트리밍 스레드 생성

    Args:
        server_config: 서버 설정 딕셔너리

    Returns:
        subprocess.Popen 객체
    """
    try:
        # 로그 디렉토리 생성
        Path('logs').mkdir(exist_ok=True)

        # 환경변수 설정
        env = os.environ.copy()
        env['PYTHONUNBUFFERED'] = '1'  # Python 출력 버퍼링 비활성화

        # PYTHONPATH에 현재 디렉토리 추가 (모듈 인식을 위해)
        current_dir = os.getcwd()
        if 'PYTHONPATH' in env:
            env['PYTHONPATH'] = f"{current_dir}:{env['PYTHONPATH']}"
        else:
            env['PYTHONPATH'] = current_dir

        # 프로세스 시작
        print_log(
            'LAUNCHER',
            f"실행 명령: {' '.join(server_config['command'])}",
            Colors.WHITE
        )

        process = subprocess.Popen(
            server_config['command'],
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            env=env,
            cwd=os.getcwd()  # 현재 작업 디렉토리 명시
        )

        # 로그 스트리밍 스레드 시작
        thread = threading.Thread(
            target=stream_output,
            args=(process, server_config['name'], server_config['color'], server_config['log']),
            daemon=True
        )
        thread.start()
        threads.append(thread)

        # 잠시 대기하여 서버가 정상 시작되는지 확인
        time.sleep(0.5)

        if process.poll() is not None:
            # 서버가 이미 종료됨
            print_log(
                'LAUNCHER',
                f"❌ {server_config['name']} 즉시 종료됨 (Exit Code: {process.returncode})",
                Colors.RED
            )
            print_log(
                'LAUNCHER',
                f"로그 파일 확인: {server_config['log']}",
                Colors.YELLOW
            )
            return None

        print_log(
            'LAUNCHER',
            f"✅ {server_config['name']} 시작됨 (PID: {process.pid})",
            Colors.GREEN
        )

        return process

    except Exception as e:
        print_log('LAUNCHER', f"❌ {server_config['name']} 시작 실패: {e}", Colors.RED)
        return None


def signal_handler(sig, frame):
    """Ctrl+C 핸들러: 모든 서버를 안전하게 종료"""
    print(f"\n{Colors.BOLD}{Colors.YELLOW}🛑 서버 종료 중...{Colors.RESET}")

    for i, proc in enumerate(processes):
        if proc and proc.poll() is None:  # 프로세스가 아직 살아있으면
            print_log('LAUNCHER', f"서버 종료 중 (PID: {proc.pid})", Colors.YELLOW)
            proc.terminate()

    # 3초 대기 후 강제 종료
    time.sleep(3)
    for proc in processes:
        if proc and proc.poll() is None:
            print_log('LAUNCHER', f"강제 종료 (PID: {proc.pid})", Colors.RED)
            proc.kill()

    print(f"{Colors.GREEN}✅ 모든 서버가 종료되었습니다.{Colors.RESET}")
    sys.exit(0)


def check_process_status():
    """주기적으로 프로세스 상태 확인"""
    while True:
        time.sleep(10)
        for i, proc in enumerate(processes):
            if proc and proc.poll() is not None:
                # 프로세스가 종료됨
                print_log(
                    'LAUNCHER',
                    f"⚠️  서버가 예기치 않게 종료됨 (PID: {proc.pid}, Exit Code: {proc.returncode})",
                    Colors.RED
                )


def main():
    """메인 함수"""

    # 현재 Python 실행 파일 경로 (가상환경 지원)
    python_executable = sys.executable

    # 서버 설정
    servers = [
        {
            'name': 'SEGMENTATION',
            'command': [python_executable, '-m', 'src.api.cloth_segmentation_api'],
            'port': 8002,
            'log': 'logs/segmentation.log',
            'color': Colors.BLUE
        },
        {
            'name': 'INPAINTING',
            'command': [python_executable, '-m', 'src.api.inpainting_api'],
            'port': 8003,
            'log': 'logs/inpainting.log',
            'color': Colors.CYAN
        },
        {
            'name': 'TRYON',
            'command': [python_executable, '-m', 'src.api.outfit_tryon_api'],
            'port': 5001,
            'log': 'logs/tryon.log',
            'color': Colors.MAGENTA
        },
        {
            'name': 'WORKER',
            'command': [python_executable, '-m', 'src.worker.cloth_processing_worker'],
            'port': None,
            'log': 'logs/worker.log',
            'color': Colors.YELLOW
        }
    ]

    # 헤더 출력
    print(f"\n{Colors.BOLD}{Colors.GREEN}{'='*70}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}🚀 ClosetConnect AI Model Servers Launcher{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}{'='*70}{Colors.RESET}\n")

    # Ctrl+C 핸들러 등록
    signal.signal(signal.SIGINT, signal_handler)

    # 각 서버 시작
    for server_config in servers:
        proc = start_server(server_config)
        if proc:
            processes.append(proc)
        time.sleep(1)  # 서버 간 시작 간격

    print(f"\n{Colors.BOLD}{Colors.GREEN}{'='*70}{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}✅ 모든 서버가 시작되었습니다!{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.WHITE}   Ctrl+C를 눌러 모든 서버를 종료할 수 있습니다.{Colors.RESET}")
    print(f"{Colors.BOLD}{Colors.GREEN}{'='*70}{Colors.RESET}\n")

    # 프로세스 상태 모니터링
    check_process_status()


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        pass
