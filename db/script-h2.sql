create table ARCHIVE_FILE
(
    ARCHIVE_FILE_ID BIGINT auto_increment,
    ARCHIVE_PATH    VARCHAR(512)         not null,
    FOLDER          BOOLEAN default FALSE,
    FILE_PATH       VARCHAR(512)         not null,
    CRC32           VARCHAR(16),
    SIZE            BIGINT,
    SHA1            VARCHAR(64),
    MD5             VARCHAR(32),
    DAT_PATH        VARCHAR(512)         not null,
    ADDED           TIMESTAMP            not null,
    MISSING         BOOLEAN default TRUE not null,
    constraint ARCHIVE_FILE_PK
        primary key (ARCHIVE_FILE_ID)
);

create index ARCHIVE_FILE_SIZE_CRC32_MISSING_INDEX
    on ARCHIVE_FILE (SIZE, CRC32, MISSING);

create index ARCHIVE_FILE_SHA1_MISSING_INDEX
    on ARCHIVE_FILE (SHA1, MISSING);

create index ARCHIVE_FILE_DAT_PATH_INDEX
    on ARCHIVE_FILE (DAT_PATH);

create index ARCHIVE_FILE_ARCHIVE_PATH_INDEX
    on ARCHIVE_FILE (ARCHIVE_PATH);

create table OUTPUT_FOLDER
(
    RELATIVE_PATH    VARCHAR(512) not null,
    SCANNED_DATE     TIMESTAMP,
    OUTPUT_FOLDER_ID BIGINT auto_increment,
    constraint OUTPUT_FOLDER_PK
        primary key (OUTPUT_FOLDER_ID)
);

create unique index OUTPUT_FOLDER_RELATIVE_PATH_UINDEX
    on OUTPUT_FOLDER (RELATIVE_PATH);

create table ARCHIVE
(
    ARCHIVE_ID             BIGINT auto_increment,
    ARCHIVE_PATH           VARCHAR(512)                  not null,
    DAT_PATH               VARCHAR(512)                  not null,
    STATUS                 VARCHAR(32) default 'MISSING' not null,
    CONTENT_SIGNATURE_SHA1 VARCHAR(128),
    constraint ARCHIVE_PK
        primary key (ARCHIVE_ID)
);

create unique index ARCHIVE_ARCHIVE_PATH_UINDEX
    on ARCHIVE (ARCHIVE_PATH);

create index ARCHIVE_DAT_PATH_INDEX
    on ARCHIVE (DAT_PATH);

create index ARCHIVE_CONTENT_SIGNATURE_SHA1_INDEX
    on ARCHIVE (CONTENT_SIGNATURE_SHA1);
