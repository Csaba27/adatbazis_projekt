# Adatbázis (MongoDB) könyvtár könyv nyilvántartó java alkalmazás

Funkciók:
- Könyvek hozzáadása, szerkesztése, törlése (példányokkal együtt), kategóriák csatolása, könyv táblázat sorának másolása
- Könyvek között keresés (cím, szerző alapján)
- Kategóriák hozzáadása, szerkesztése, törlése
- példányok hozzáadása, szerkesztése, törlése

Vizuális felület (javafx GUI):
- Tab content: könyvek, könyv kategóriák, dupla kattintás könyvek táblázaton megnyitja a könyv összes példányát

Adatbázis:
- 3 tábla books, book_copies, categories
- books tábla minden eleme categories tulajdonsága tárolja a kategóriak azonosítóját ( array ObjectId)
- book_copies minden eleme szigoruan tartalmazza a könyv azonosítóját (ObjectId)
- MongoDB java kliens mongodb.driver.sync csomag segítségével

<br />

Adatbázis szerkezet

![image](https://github.com/user-attachments/assets/01366857-546f-4842-a15c-34fc61cda362)

Program

![image](https://github.com/user-attachments/assets/eda9c694-8146-4c07-8a04-5bcd81e46f1a)

![image](https://github.com/user-attachments/assets/4f942159-3a06-475a-be18-b809a6ff3959)

![image](https://github.com/user-attachments/assets/c322c3c8-5290-42ed-a0f3-e5a8b64a3a85)

![image](https://github.com/user-attachments/assets/83e08e3b-81ea-4261-ae5f-55e4023b1090)


Kategória
![image](https://github.com/user-attachments/assets/edd35983-b8cb-4fc1-b149-88a0a6746ac7)

Könyv
![image](https://github.com/user-attachments/assets/2743661c-e327-42b7-b1eb-fdfdea110085)

Példány
![image](https://github.com/user-attachments/assets/a5d6b30e-9fbe-4c1c-b14a-cdbaa96b5d5c)
