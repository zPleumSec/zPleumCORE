# zPleumCORE # Version 1.2.4

## รายละเอียดโปรเจกต์
**ชื่อโปรเจกต์:** zPleumCORE  
**เวอร์ชัน:** 1.2.4  
**ผู้พัฒนา:** zPleum  
**คำอธิบาย:** ระบบจัดการสิทธิ์ OP, คำสั่งลับ และการตั้งค่าความปลอดภัยสำหรับเซิร์ฟเวอร์ Minecraft ที่เน้นความปลอดภัยและความง่ายในการใช้งาน  
**เว็บไซต์:** [https://zpleum.site/](https://zpleum.site/)  
**API-Version:** 1.21  
**Main Class:** `me.zpleum.zPleumCORE.ZPleumCORE`

---

## คำสั่ง (Commands)

### 1. `/zpleumcoreverify`
- **คำอธิบาย:** ให้ผู้เล่นสามารถ OP ตัวเองได้ด้วยรหัสลับ  
- **สิทธิ์:** `zpleumcore.verify.use`  
- **ข้อความแจ้งเมื่อไม่มีสิทธิ์:** §cYou do not have permission to use this command.  
- **Usage:** ใช้รหัสลับเพื่อยืนยันและรับสิทธิ์ OP

---

### 2. `/zpleumcorereset`
- **คำอธิบาย:** ให้ผู้เล่นลบสถานะ OP ทั้งหมดด้วยรหัสลับ  
- **สิทธิ์:** `zpleumcore.verify.reset`  
- **ข้อความแจ้งเมื่อไม่มีสิทธิ์:** §cYou do not have permission to use this command.  
- **Usage:** ใช้รหัสลับเพื่อล้างสถานะ OP

---

### 3. `/zpleumcoreexclusive`
- **คำอธิบาย:** ให้ผู้เล่นใช้คำสั่ง OP และ LuckPerms ได้  
- **สิทธิ์:** `zpleumcore.verify.exclusive`  
- **ข้อความแจ้งเมื่อไม่มีสิทธิ์:** §cYou do not have permission to use this command.  
- **Usage:** ใช้รหัสพิเศษเพื่อเข้าถึงคำสั่งขั้นสูง

---

### 4. `/zpleumcorereload`
- **คำอธิบาย:** ให้ผู้เล่นโหลดค่าคอนฟิกใหม่โดยไม่ต้องรีสตาร์ทเซิร์ฟเวอร์  
- **สิทธิ์:** `zpleumcore.verify.reload`  
- **ข้อความแจ้งเมื่อไม่มีสิทธิ์:** §cYou do not have permission to use this command.  
- **Usage:** รีโหลดการตั้งค่าทั้งหมดแบบสดๆ

---

## สิทธิ์การใช้งาน (Permissions)

| Permission                   | คำอธิบาย                             | ค่าเริ่มต้น (Default) |
|------------------------------|-------------------------------------|-----------------------|
| `zpleumcore.verify.use`       | สิทธิ์ใช้คำสั่ง `/zpleumcoreverify` | op                    |
| `zpleumcore.verify.reset`     | สิทธิ์ใช้คำสั่ง `/zpleumcorereset`  | op                    |
| `zpleumcore.verify.exclusive` | สิทธิ์ใช้คำสั่ง `/zpleumcoreexclusive` | op                 |
| `zpleumcore.verify.reload`    | สิทธิ์ใช้คำสั่ง `/zpleumcorereload` | op                    |

---

## คำอธิบายฟีเจอร์หลัก

- **ระบบรหัสลับ OP/Reset:**  
  ใช้รหัสพิเศษสำหรับให้ผู้เล่น OP ตัวเอง หรือ รีเซ็ตสถานะ OP ผ่านคำสั่งลับ ป้องกันการใช้งานโดยไม่ได้รับอนุญาต

- **การควบคุมสิทธิ์ขั้นสูง:**  
  กำหนดสิทธิ์แบบละเอียดสำหรับคำสั่งแต่ละตัว ป้องกันการเข้าถึงคำสั่งที่สำคัญโดยผู้เล่นทั่วไป

- **ระบบรีโหลดคอนฟิก:**  
  สามารถรีโหลดค่าคอนฟิกได้แบบไม่ต้องรีสตาร์ทเซิร์ฟเวอร์ ช่วยให้การปรับแต่งสะดวกและรวดเร็ว

- **รองรับ LuckPerms:**  
  รองรับการจัดการสิทธิ์ร่วมกับปลั๊กอิน LuckPerms โดยเฉพาะสำหรับคำสั่ง exclusive

---

## วิธีการติดตั้ง

1. ดาวน์โหลดไฟล์ `zPleumCORE.jar` และวางไว้ในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์ Minecraft  
2. รีสตาร์ทเซิร์ฟเวอร์ หรือใช้คำสั่ง `/zpleumcorereload` เพื่อตั้งค่าใหม่โดยไม่ต้องรีสตาร์ท  
3. กำหนดสิทธิ์ให้กับผู้เล่นหรือกลุ่มผู้เล่นผ่านระบบ Permission Plugin ที่ใช้ (เช่น LuckPerms) ตามคำอธิบายในหัวข้อ Permissions  

---

## ตัวอย่างการใช้งาน

- ผู้เล่นที่ได้รับสิทธิ์ `zpleumcore.verify.use` สามารถใช้คำสั่ง:  

## เพื่อรับ OP ตัวเอง  

- ผู้เล่นที่ได้รับสิทธิ์ `zpleumcore.verify.reset` สามารถใช้คำสั่ง:  

## เพื่อลบสถานะ OP ทั้งหมด  

- ผู้เล่นที่ได้รับสิทธิ์ `zpleumcore.verify.exclusive` จะสามารถใช้คำสั่งที่เกี่ยวกับ OP และ LuckPerms ได้ทั้งหมด  

- ผู้เล่นที่ได้รับสิทธิ์ `zpleumcore.verify.reload` สามารถรีโหลดค่าคอนฟิกได้ทันทีโดยใช้คำสั่ง:  

---

## ติดต่อและข้อมูลเพิ่มเติม
- เว็บไซต์หลัก: [https://zpleum.site/](https://zpleum.site/)  
- ผู้พัฒนา: zPleum, undertailx
- ลิขสิทธิ์: zPleum all copyrights reserved.

---

## License
โปรเจกต์นี้สงวนสิทธิ์การใช้งานทั้งหมด (All rights reserved)  
กรุณาอย่าก๊อปปี้หรือแจกจ่ายโดยไม่ได้รับอนุญาต
