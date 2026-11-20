-- Usuarios iniciales SOLO PARA DESARROLLO (datos ficticios).
-- Contraseña de todos: SeaShare2026!  (guardada con hash BCrypt, nunca en texto plano).
-- Inicio de sesión: número de documento + contraseña.
INSERT INTO users (id, full_name, document_number, email, phone, password, role)
VALUES ('2909ed26-b8ba-4e05-8f51-744d95ca9417', 'Laura Gómez Restrepo', 1010000001,
        'admin@seashare.test', '3001000001',
        '$2b$10$CNjRBtjvvJMxo1lsMCgI4OuWF5XdMC8oQpcv47uk5GXA9blQ675Se', 'ADMIN'),
       ('2e38dc8e-a473-4ea7-9c3e-fbda92fcce98', 'Carlos Andrés Mejía', 1010000002,
        'carlos.mejia@seashare.test', '3001000002',
        '$2b$10$27vOwCzfhr8Po.t5ACsuOOoH96gAyKlYSoAYNYw118cxw3l21lyC.', 'OWNER'),
       ('0d2a784d-dcb1-4d99-93b5-1b5b4719b6ea', 'María Fernanda Ospina', 1010000003,
        'maria.ospina@seashare.test', '3001000003',
        '$2b$10$w6dCNxXnILZc/TmCWm8lo.MltzwX0uK6eebTMtrVpfiNIZdSEecqO', 'OWNER'),
       ('448fcb7e-187f-406b-8f9d-b0f8547954de', 'Juan Pablo Cardona', 1010000004,
        'juan.cardona@seashare.test', '3001000004',
        '$2b$10$0/.HsDlSzdUdyFi1V3X2L.hllC9lbTLlZ8JdfNK9qv./zZVM9/raW', 'OWNER'),
       ('0495eab5-b04f-4a0f-9e0d-967bb889d260', 'Sofía Valencia Duque', 1010000005,
        'sofia.valencia@seashare.test', '3001000005',
        '$2b$10$UFR01Yt0HJdfwTcCHvFjoe96HQYQgkO0X2P53mvbMJcLecuzvIpUi', 'OWNER'),
       ('080bd4a8-09af-4a7c-b46c-e402fe01b82a', 'Andrés Felipe Londoño', 1010000006,
        'andres.londono@seashare.test', '3001000006',
        '$2b$10$CIwbf6PGOHcJaeTiLUuWCusmaj6jjlccF8PgjRbPRwNmbHcwt5Joa', 'OWNER');
