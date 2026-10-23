# meta-kv260 EDF Yocto Layer Template

AMD Kria KV260 보드를 위한 EDF(Embedded Development Framework) Yocto 레이어입니다. SDT(System Device Tree)로부터 생성한 머신 BSP 설정과 PL 펌웨어, 커스텀 이미지 레시피를 함께 관리합니다. AMD Vivado Tool를 사용하여 디자인한 PL을 EDF Linux에 적용하기 위한 레이어입니다.


## 개발 환경

AMD EDF(Embedded Developement Framewoark) 2026.01 버전을 기반으로 개발되었습니다.

## 파일 구조

```text
meta-kv260/
├── conf/
│   ├── layer.conf
|   ├── kv260-settings.inc
|   |   -------------------- gen-machine-conf로 생성된 파일들 ---------------------
│   ├── machine/                    # 생성된 머신 설정과 include 파일
│   ├── multiconfig/                # 생성된 FSBL, PMU 펌웨어 빌드 설정
│   └── dts/                        # 생성된 Device Tree 파일
│       └── zynqmp-kv260-sdt-full/
│           ├── cortexa53-linux.dts
│           └── pl-overlay-full/
│               └── pl.dtso
|   ------------------------------------------------------------------------------
├── hw/
│   └── sdt/                        # 입력 SDT 전체와 .bit 파일
├── recipes-core/
│   └── images/
│       └── kv260-image.bb
├── recipes-firmware/
│   └── kv260-pl-firmware/
│       └── kv260-pl-firmware_1.0.bb
└── scripts/
    └── gen-machine.sh
```
<br>

> [WARNING]  
> **`gen-machine-conf`로 생성된 파일은 직접 수정하지 마세요.**
> 스크립트를 다시 실행하면 수동 변경 내용이 덮어써질 수 있습니다. 하드웨어 변경은 `hw/sdt/`에 반영한 뒤 `gen-machine.sh`로 다시 생성하세요.

## 새로운 SDT 적용 방법
만약, PL의 변경사항이 발생한 경우 다음의 순서를 통해 Layer에 적용합니다.
1. XSA로부터 SDT를 추출하여 SDT내 파일들을 'sdt'폴더 안에 복사하여 내용을 교채합니다.
2. EDF의 Yocto 개발 환경울 활성화합니다.
    ```bash
    source ./edf-init-build-env build
    ```
3. gen-machine.sh를 실행하여 meta-kv260 Layer에 새로운 SDT를 적용합니다.
    ```bash
    ../sources/meta-kv260/scripts/gen-machine.sh
    ```

<br>

## KV260 설정
### 1. KV260_MACHINE_NAME
Yocto에서 사용할 MACHINE의 이름을 설정


## 이미지 레시피의 역할

### 1. kv260-image.bb

[kv260-image.bb](recipes-core/images/kv260-image.bb)는 SDT 기반 KV260 Linux 이미지의 구성을 정의합니다.

### 2. kv260-pl-firmware_1.0.bb

[kv260-pl-firmware_1.0.bb](recipes-firmware/kv260-pl-firmware/kv260-pl-firmware_1.0.bb)는 SDT의 `.bit`와 생성된 `.dtso`를 `.bin`, `.dtbo`로 변환하여 Linux 이미지에 포함할 PL 펌웨어를 패키징합니다.

두 레시피는 `zynqmp-kv260-sdt-full` 머신을 대상으로 합니다. 이미지의 루트 파일 시스템에는 다음 PL 파일이 설치됩니다.

```text
/lib/firmware/xilinx/kv260-pl-firmware/
├── kv260-pl-firmware.bin
└── kv260-pl-firmware.dtbo
```

`.bin`은 FPGA의 PL 회로를 구성하는 비트스트림이고, `.dtbo`는 해당 PL 장치를 Linux에 등록하는 디바이스 트리 오버레이입니다. `kv260-pl-autoload` 서비스가 부팅 시 FPGA Manager를 통해 두 파일을 로드합니다.

## PCAM 5C 초기화 및 GStreamer 송수신 예제

PCAM 5C의 OV5640 센서에서 받은 영상을 CSI-2 RX와 AXI VDMA를 통해 캡처하고, KV260에서 JPEG으로 압축하여 RTP/UDP로 송신합니다. Windows PC의 GStreamer로 640×480, 30 fps 송수신을 확인한 예제입니다.

```text
PCAM 5C (OV5640) → CSI-2 RX → AXI VDMA → /dev/video0
    → GStreamer (JPEG/RTP/UDP) → Windows PC
```

`kv260-image`에는 OV5640 드라이버 설정과 VDMA packed YUYV 캡처 패치, 카메라 설정 도구 및 송신용 GStreamer 플러그인이 포함됩니다. `pcam-user.dtsi`는 빌드용 `pl.dtso` 뒤에 include되어 하나의 PL 오버레이로 컴파일됩니다.

### 1. KV260에서 PL 및 카메라 인식 확인

아래 명령은 KV260의 Linux 셸에서 실행합니다. PowerShell로 SSH 접속한 경우에도 접속 후에는 Linux 명령을 사용합니다.

```bash
systemctl status kv260-pl-autoload --no-pager
sudo journalctl -u kv260-pl-autoload -b --no-pager
v4l2-ctl --list-devices
media-ctl -d /dev/media0 -p
```

PL 로딩 서비스가 `active (exited)`이고 종료 코드가 `0/SUCCESS`이면 로딩이 완료된 것입니다. 아직 시작되지 않았다면 다음을 실행합니다.

```bash
sudo systemctl start kv260-pl-autoload
```

현재 보드에서 확인한 장치와 entity 이름은 다음과 같습니다. 장치 번호는 환경에 따라 달라질 수 있으므로 위 출력과 다르면 이후 명령도 맞춰 변경하세요.

| 항목 | 장치 또는 entity 이름 |
|---|---|
| Media Controller | `/dev/media0` |
| 캡처 장치 | `/dev/video0` |
| OV5640 센서 | `ov5640 6-003c` (`/dev/v4l-subdev1`) |
| CSI-2 RX | `a0030000.mipi_csi2_rx_subsystem` (`/dev/v4l-subdev0`) |

PCAM의 PWUP GPIO는 디바이스 트리의 regulator가 제어하고, 센서 레지스터 초기화는 OV5640 드라이버가 담당합니다. 별도의 `gpioset`이나 `i2cset` 초기화는 필요하지 않습니다.

### 2. KV260에서 해상도 및 캡처 포맷 설정

스트리밍을 시작하기 전에 센서와 CSI의 해상도를 모두 640×480으로 맞춥니다. 현재 미디어 그래프의 링크는 이미 활성화되어 있습니다.

```bash
sudo media-ctl -d /dev/media0 --set-v4l2 \
  '"ov5640 6-003c":0 [fmt:UYVY8_1X16/640x480 field:none]'

sudo media-ctl -d /dev/media0 --set-v4l2 \
  '"a0030000.mipi_csi2_rx_subsystem":0 [fmt:UYVY8_1X16/640x480 field:none]'

sudo v4l2-ctl -d /dev/video0 \
  --set-fmt-video=width=640,height=480,pixelformat=YUYV

sudo media-ctl -d /dev/media0 -p
sudo v4l2-ctl -d /dev/video0 --get-fmt-video
```

센서와 CSI의 입력·출력 pad가 모두 `640x480`이고 센서의 프레임 간격이 `@1/30`인지 확인합니다. 캡처 장치는 `YUYV`여야 합니다. 현재 VDMA 패치 기준으로 포맷 이름은 다음처럼 구분됩니다.

| 위치 | 포맷 |
|---|---|
| 센서 및 CSI media bus | `UYVY8_1X16` |
| V4L2 캡처 메모리 | `YUYV` |
| GStreamer caps | `YUY2` |

### 3. KV260에서 캡처 테스트

```bash
sudo v4l2-ctl -d /dev/video0 \
  --stream-mmap=4 \
  --stream-count=60 \
  --stream-to=/dev/null
```

60프레임을 받고 종료되면 다음 단계로 진행합니다. 프레임을 받지 못하거나 오류가 발생하면 송신 전에 센서 인식, 미디어 그래프의 포맷 및 커널 로그를 확인하세요.

```bash
sudo dmesg | grep -Ei 'ov5640|csi|vdma|xilinx-video|pcam'
```

### 4. Windows PC에서 수신 시작 (PowerShell)

수신 PC에는 GStreamer와 JPEG/RTP 및 화면 출력 플러그인이 설치되어 있어야 하며, `gst-launch-1.0`이 PATH에 등록되어 있어야 합니다. Windows 방화벽에서 GStreamer의 수신 접근(UDP 5000)을 허용하세요.

PowerShell에서 아래 한 줄 명령을 먼저 실행합니다.

```powershell
gst-launch-1.0 -v udpsrc port=5000 caps="application/x-rtp,media=video,encoding-name=JPEG,clock-rate=90000,payload=26" ! rtpjitterbuffer latency=100 ! rtpjpegdepay ! jpegdec ! videoconvert ! autovideosink sync=false
```

### 5. KV260에서 카메라 영상 송신

아래 명령은 KV260의 Linux 셸에서 실행합니다. 수신 PC의 IP는 `192.168.0.12`, UDP 포트는 `5000`을 사용합니다. 다른 PC에서 수신한다면 `host` 값을 변경하세요.

```bash
sudo gst-launch-1.0 -v v4l2src device=/dev/video0 io-mode=mmap ! 'video/x-raw,format=YUY2,width=640,height=480,framerate=30/1' ! videoconvert ! 'video/x-raw,format=I420' ! jpegenc quality=75 ! rtpjpegpay pt=26 mtu=1400 ! udpsink host=192.168.0.12 port=5000 sync=false async=false
```

송신과 수신은 각 터미널에서 `Ctrl+C`로 종료합니다. 재부팅 후에는 해상도 및 캡처 포맷 설정부터 다시 실행하세요.

### 6. 카메라 없이 네트워크 송수신 확인

카메라 대신 테스트 패턴을 송신하려면 KV260에서 다음을 실행합니다. Windows PC의 수신 명령은 동일합니다.

```bash
gst-launch-1.0 -v videotestsrc is-live=true ! 'video/x-raw,format=I420,width=640,height=480,framerate=30/1' ! jpegenc quality=75 ! rtpjpegpay pt=26 mtu=1400 ! udpsink host=192.168.0.12 port=5000 sync=false async=false
```
