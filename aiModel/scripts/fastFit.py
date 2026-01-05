from gradio_client import Client, handle_file
from PIL import Image
import os

GRADIO_URL = "https://716905348439c28a49.gradio.live"
API_NAME = "/generation_wrapper"

# "비어있는 이미지 입력"으로 인식되도록 하는 dict
EMPTY_IMAGE = {
    "path": None,
    "url": None,
    "size": None,
    "orig_name": None,
    "mime_type": None,
    "is_stream": False,
    "meta": {}
}

def ensure_dummy(path: str):
    if os.path.exists(path):
        return
    Image.new("RGBA", (1, 1), (0, 0, 0, 0)).save(path, "PNG")

def main():
    client = Client(GRADIO_URL)

    person = "./test/1.png"
    top = "./test/2.png"
    bottom = "./test/3.png"

    # shoes/bag는 선택이지만, /generation_wrapper가 required라 일단 더미로 채움
    shoes = "dummy_shoes.png"
    bag = "dummy_bag.png"
    ensure_dummy(shoes)
    ensure_dummy(bag)

    result_img, status_html = client.predict(
        handle_file(person),  # param_1 (사람)
        handle_file(top),     # param_2 (상의)
        handle_file(bottom),  # param_3 (하의)
        EMPTY_IMAGE,          # param_4 (드레스 슬롯 비움) ★중요
        handle_file(shoes),   # param_5 (신발 더미)
        handle_file(bag),     # param_6 (가방 더미)
        512,    # param_7 (참조 이미지 크기)
        30,     # param_8 (steps)
        2.5,    # param_9 (guidance)
        False,  # param_10 (square mask)
        42,     # param_11 (seed)
        True,   # param_12 (pose guide)
        api_name=API_NAME
    )

    print("status:", status_html)
    print("result_img:", result_img)

    # result_img가 None이면(규칙 위반/실패 등) 여기서 종료
    if not result_img:
        print("No output image generated (result_img is None).")
        return

    out_path = result_img.get("path")
    if out_path and os.path.exists(out_path):
        Image.open(out_path).save("./test/tryon_result.png")
        print("saved: ./test/tryon_result.png")
    else:
        print("No output file path returned.")

if __name__ == "__main__":
    main()
