USE practicas_pedagogicas;

UPDATE usuario SET password_hash = '$2a$10$jmRiGo4/n0AM.hxk5868xOHzBAunuuw9rJXRANULX7LBXF7mMOaxq'
    WHERE id_usuario IN (1, 2, 4) AND password_hash = 'HASH_PENDIENTE_LOGIN';

SELECT id_usuario, email, rol FROM usuario WHERE id_usuario IN (1, 2, 4);

SELECT id_notificacion, id_usuario, mensaje, enlace, leida, fecha FROM notificacion ORDER BY id_notificacion DESC;
