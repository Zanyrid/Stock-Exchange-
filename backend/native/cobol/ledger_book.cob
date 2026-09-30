IDENTIFICATION DIVISION.
PROGRAM-ID. LEDGER-BOOK.
AUTHOR. BURSA-ABSURD-CORE.

*> ============================================================================
*> BURSA KOMODITAS ABSURD - BUKU BESAR (GnuCOBOL, format bebas)
*>
*> Pemakaian:
*>   ledger_book RECORD <tx_id> <timestamp> <trader> <symbol> <side> <qty> <price>
*>   ledger_book REPORT     laporan kolom tetap ala mainframe
*>   ledger_book AUDIT      cek konsistensi qty x harga = total (keluaran JSON)
*>
*> Data disimpan di "ledger.dat" pada direktori kerja saat program dijalankan.
*> ============================================================================

ENVIRONMENT DIVISION.
INPUT-OUTPUT SECTION.
FILE-CONTROL.
    SELECT LEDGER-FILE ASSIGN TO "ledger.dat"
        ORGANIZATION IS LINE SEQUENTIAL
        FILE STATUS IS WS-FILE-STATUS.

DATA DIVISION.
FILE SECTION.
FD  LEDGER-FILE.
01  LEDGER-RECORD.
    05  REC-TX-ID           PIC X(12).
    05  REC-TIMESTAMP       PIC X(19).
    05  REC-TRADER          PIC X(15).
    05  REC-SYMBOL          PIC X(08).
    05  REC-SIDE            PIC X(04).
    05  REC-QTY             PIC 9(05).
    05  REC-PRICE           PIC 9(07)V99.
    05  REC-TOTAL           PIC 9(09)V99.

WORKING-STORAGE SECTION.
01  WS-FILE-STATUS          PIC X(02).
01  WS-COMMAND              PIC X(10).
01  WS-ARG-COUNT            PIC 9(03) VALUE ZERO.
01  WS-EOF-FLAG             PIC X(01) VALUE 'N'.
    88  END-OF-FILE                   VALUE 'Y'.

*> Penerima argumen CLI untuk perintah RECORD
01  CLI-TX-ID               PIC X(12).
01  CLI-TIMESTAMP           PIC X(19).
01  CLI-TRADER              PIC X(15).
01  CLI-SYMBOL              PIC X(08).
01  CLI-SIDE                PIC X(04).
01  CLI-QTY-STR             PIC X(10).
01  CLI-PRICE-STR           PIC X(12).

01  WS-QTY-NUM              PIC 9(05).
01  WS-PRICE-NUM            PIC 9(07)V99.
01  WS-TOTAL-CALC           PIC 9(09)V99.
01  WS-CHECK-TOTAL          PIC 9(09)V99.

*> Akumulator laporan
01  WS-TOTAL-RECORDS        PIC 9(06) VALUE ZERO.
01  WS-TOTAL-VOLUME         PIC 9(08) VALUE ZERO.
01  WS-GRAND-TURNOVER       PIC 9(12)V99 VALUE ZERO.
01  WS-BAD-COUNT            PIC 9(06) VALUE ZERO.
01  WS-OUT-COUNT            PIC Z(5)9.
01  WS-OUT-BAD              PIC Z(5)9.
01  WS-OUT-COUNT-X          PIC X(06).
01  WS-OUT-BAD-X            PIC X(06).

*> Tabel subtotal per komoditas
01  WS-SYMBOL-TABLE.
    05  WS-SYM-ENTRY OCCURS 20 TIMES.
        10  WS-SYM-NAME     PIC X(08).
        10  WS-SYM-COUNT    PIC 9(06).
        10  WS-SYM-VOLUME   PIC 9(08).
        10  WS-SYM-TURNOVER PIC 9(12)V99.
01  WS-SYM-USED             PIC 9(02) VALUE ZERO.
01  WS-IDX                  PIC 9(02) VALUE ZERO.
01  WS-FOUND-FLAG           PIC X(01) VALUE 'N'.
    88  SYMBOL-FOUND                  VALUE 'Y'.

*> Baris laporan (lebar 98 kolom)
01  RPT-DIVIDER.
    05 FILLER               PIC X(98) VALUE ALL "=".
01  RPT-SUBDIVIDER.
    05 FILLER               PIC X(98) VALUE ALL "-".

01  RPT-HEADER-1.
    05 FILLER               PIC X(27) VALUE SPACES.
    05 FILLER               PIC X(43)
       VALUE "BURSA KOMODITAS ABSURD - LAPORAN BUKU BESAR".
    05 FILLER               PIC X(28) VALUE SPACES.

01  RPT-COL-HEAD.
    05 FILLER               PIC X(12) VALUE "TX ID".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(19) VALUE "TIMESTAMP".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(15) VALUE "TRADER BOT".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(09) VALUE "KOMODITAS".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(04) VALUE "SIDE".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(06) VALUE "   QTY".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(12) VALUE "       HARGA".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(14) VALUE "         TOTAL".

01  RPT-DETAIL-LINE.
    05 DTL-TX-ID            PIC X(12).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-TIMESTAMP        PIC X(19).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-TRADER           PIC X(15).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-SYMBOL           PIC X(09).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-SIDE             PIC X(04).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-QTY              PIC ZZ,ZZ9.
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-PRICE            PIC Z,ZZZ,ZZ9.99.
    05 FILLER               PIC X(01) VALUE SPACE.
    05 DTL-TOTAL            PIC ZZZ,ZZZ,ZZ9.99.

01  RPT-SYM-HEAD.
    05 FILLER               PIC X(10) VALUE "KOMODITAS".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(07) VALUE "  JUMLAH".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(10) VALUE "    VOLUME".
    05 FILLER               PIC X(01) VALUE SPACE.
    05 FILLER               PIC X(18) VALUE "          TURNOVER".

01  RPT-SYM-LINE.
    05 SYL-NAME             PIC X(10).
    05 FILLER               PIC X(01) VALUE SPACE.
    05 SYL-COUNT            PIC ZZZ,ZZ9.
    05 FILLER               PIC X(01) VALUE SPACE.
    05 SYL-VOLUME           PIC ZZ,ZZZ,ZZ9.
    05 FILLER               PIC X(01) VALUE SPACE.
    05 SYL-TURNOVER         PIC ZZZ,ZZZ,ZZZ,ZZ9.99.

01  RPT-SUMMARY-LINE.
    05 FILLER               PIC X(18) VALUE "TOTAL TRANSAKSI : ".
    05 SMR-RECORDS          PIC ZZZ,ZZ9.
    05 FILLER               PIC X(05) VALUE SPACES.
    05 FILLER               PIC X(15) VALUE "TOTAL VOLUME : ".
    05 SMR-VOLUME           PIC ZZ,ZZZ,ZZ9.
    05 FILLER               PIC X(05) VALUE SPACES.
    05 FILLER               PIC X(12) VALUE "TURNOVER : ".
    05 SMR-TURNOVER         PIC ZZZ,ZZZ,ZZZ,ZZ9.99.

PROCEDURE DIVISION.
0000-MAIN-LOGIC.
    ACCEPT WS-ARG-COUNT FROM ARGUMENT-NUMBER
    IF WS-ARG-COUNT < 1
        PERFORM 9000-SHOW-USAGE
    ELSE
        ACCEPT WS-COMMAND FROM ARGUMENT-VALUE
        MOVE FUNCTION UPPER-CASE(WS-COMMAND) TO WS-COMMAND
        EVALUATE WS-COMMAND
            WHEN "RECORD"
                PERFORM 1000-PROCESS-RECORD
            WHEN "REPORT"
                PERFORM 2000-PROCESS-REPORT
            WHEN "AUDIT"
                PERFORM 3000-AUDIT-LEDGER
            WHEN OTHER
                PERFORM 9000-SHOW-USAGE
        END-EVALUATE
    END-IF
    STOP RUN.

*> ----------------------------------------------------------------------------
*> PENCATATAN TRANSAKSI (APPEND KE FILE BUKU BESAR)
*> ----------------------------------------------------------------------------
1000-PROCESS-RECORD.
    IF WS-ARG-COUNT < 8
        DISPLAY '{"status":"ERROR","reason":"RECORD butuh 7 argumen"}'
        MOVE 2 TO RETURN-CODE
    ELSE
        PERFORM 1100-READ-RECORD-ARGS
        PERFORM 1200-WRITE-LEDGER-ROW
    END-IF.

1100-READ-RECORD-ARGS.
    ACCEPT CLI-TX-ID       FROM ARGUMENT-VALUE
    ACCEPT CLI-TIMESTAMP   FROM ARGUMENT-VALUE
    ACCEPT CLI-TRADER      FROM ARGUMENT-VALUE
    ACCEPT CLI-SYMBOL      FROM ARGUMENT-VALUE
    ACCEPT CLI-SIDE        FROM ARGUMENT-VALUE
    ACCEPT CLI-QTY-STR     FROM ARGUMENT-VALUE
    ACCEPT CLI-PRICE-STR   FROM ARGUMENT-VALUE

    COMPUTE WS-QTY-NUM   = FUNCTION NUMVAL(CLI-QTY-STR)
    COMPUTE WS-PRICE-NUM = FUNCTION NUMVAL(CLI-PRICE-STR)
    COMPUTE WS-TOTAL-CALC = WS-QTY-NUM * WS-PRICE-NUM.

1200-WRITE-LEDGER-ROW.
    OPEN EXTEND LEDGER-FILE
    IF WS-FILE-STATUS NOT = "00" AND WS-FILE-STATUS NOT = "05"
        OPEN OUTPUT LEDGER-FILE
    END-IF

    IF WS-FILE-STATUS NOT = "00" AND WS-FILE-STATUS NOT = "05"
        DISPLAY '{"status":"ERROR","reason":"gagal membuka ledger.dat, status '
                WS-FILE-STATUS '"}'
        MOVE 3 TO RETURN-CODE
    ELSE
        INITIALIZE LEDGER-RECORD
        MOVE CLI-TX-ID      TO REC-TX-ID
        MOVE CLI-TIMESTAMP  TO REC-TIMESTAMP
        MOVE CLI-TRADER     TO REC-TRADER
        MOVE CLI-SYMBOL     TO REC-SYMBOL
        MOVE CLI-SIDE       TO REC-SIDE
        MOVE WS-QTY-NUM     TO REC-QTY
        MOVE WS-PRICE-NUM   TO REC-PRICE
        MOVE WS-TOTAL-CALC  TO REC-TOTAL
        WRITE LEDGER-RECORD
        IF WS-FILE-STATUS = "00"
            DISPLAY '{"status":"SUCCESS","action":"RECORDED","tx_id":"'
                    FUNCTION TRIM(CLI-TX-ID) '"}'
        ELSE
            DISPLAY '{"status":"ERROR","reason":"gagal menulis, status '
                    WS-FILE-STATUS '"}'
            MOVE 4 TO RETURN-CODE
        END-IF
        CLOSE LEDGER-FILE
    END-IF.

*> ----------------------------------------------------------------------------
*> LAPORAN BUKU BESAR BERFORMAT KOLOM TETAP
*> ----------------------------------------------------------------------------
2000-PROCESS-REPORT.
    OPEN INPUT LEDGER-FILE
    IF WS-FILE-STATUS NOT = "00"
        DISPLAY "BELUM ADA TRANSAKSI YANG DIBUKUKAN."
    ELSE
        INITIALIZE WS-SYMBOL-TABLE
        MOVE ZERO TO WS-SYM-USED
        PERFORM 2050-PRINT-HEADER
        MOVE "N" TO WS-EOF-FLAG
        PERFORM UNTIL END-OF-FILE
            READ LEDGER-FILE
                AT END
                    SET END-OF-FILE TO TRUE
                NOT AT END
                    PERFORM 2100-PRINT-DETAIL-ROW
            END-READ
        END-PERFORM
        CLOSE LEDGER-FILE
        PERFORM 2300-PRINT-SYMBOL-TOTALS
        PERFORM 2400-PRINT-GRAND-TOTAL
    END-IF.

2050-PRINT-HEADER.
    DISPLAY RPT-DIVIDER
    DISPLAY RPT-HEADER-1
    DISPLAY RPT-DIVIDER
    DISPLAY RPT-COL-HEAD
    DISPLAY RPT-SUBDIVIDER.

2100-PRINT-DETAIL-ROW.
    ADD 1 TO WS-TOTAL-RECORDS
    ADD REC-QTY TO WS-TOTAL-VOLUME
    ADD REC-TOTAL TO WS-GRAND-TURNOVER
    PERFORM 2200-ACCUMULATE-SYMBOL

    MOVE REC-TX-ID     TO DTL-TX-ID
    MOVE REC-TIMESTAMP TO DTL-TIMESTAMP
    MOVE REC-TRADER    TO DTL-TRADER
    MOVE REC-SYMBOL    TO DTL-SYMBOL
    MOVE REC-SIDE      TO DTL-SIDE
    MOVE REC-QTY       TO DTL-QTY
    MOVE REC-PRICE     TO DTL-PRICE
    MOVE REC-TOTAL     TO DTL-TOTAL
    DISPLAY RPT-DETAIL-LINE.

2200-ACCUMULATE-SYMBOL.
    MOVE 'N' TO WS-FOUND-FLAG
    PERFORM VARYING WS-IDX FROM 1 BY 1
            UNTIL WS-IDX > WS-SYM-USED OR SYMBOL-FOUND
        IF WS-SYM-NAME(WS-IDX) = REC-SYMBOL
            SET SYMBOL-FOUND TO TRUE
        END-IF
    END-PERFORM

    IF SYMBOL-FOUND
        SUBTRACT 1 FROM WS-IDX
    ELSE
        IF WS-SYM-USED < 20
            ADD 1 TO WS-SYM-USED
            MOVE WS-SYM-USED TO WS-IDX
            MOVE REC-SYMBOL TO WS-SYM-NAME(WS-IDX)
        ELSE
            MOVE ZERO TO WS-IDX
        END-IF
    END-IF

    IF WS-IDX > 0
        ADD 1 TO WS-SYM-COUNT(WS-IDX)
        ADD REC-QTY TO WS-SYM-VOLUME(WS-IDX)
        ADD REC-TOTAL TO WS-SYM-TURNOVER(WS-IDX)
    END-IF.

2300-PRINT-SYMBOL-TOTALS.
    DISPLAY RPT-SUBDIVIDER
    DISPLAY RPT-SYM-HEAD
    PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > WS-SYM-USED
        MOVE WS-SYM-NAME(WS-IDX)     TO SYL-NAME
        MOVE WS-SYM-COUNT(WS-IDX)    TO SYL-COUNT
        MOVE WS-SYM-VOLUME(WS-IDX)   TO SYL-VOLUME
        MOVE WS-SYM-TURNOVER(WS-IDX) TO SYL-TURNOVER
        DISPLAY RPT-SYM-LINE
    END-PERFORM.

2400-PRINT-GRAND-TOTAL.
    MOVE WS-TOTAL-RECORDS   TO SMR-RECORDS
    MOVE WS-TOTAL-VOLUME    TO SMR-VOLUME
    MOVE WS-GRAND-TURNOVER  TO SMR-TURNOVER
    DISPLAY RPT-SUBDIVIDER
    DISPLAY RPT-SUMMARY-LINE
    DISPLAY RPT-DIVIDER.

*> ----------------------------------------------------------------------------
*> AUDIT: PASTIKAN QTY x HARGA = TOTAL DI SETIAP BARIS
*> ----------------------------------------------------------------------------
3000-AUDIT-LEDGER.
    OPEN INPUT LEDGER-FILE
    IF WS-FILE-STATUS NOT = "00"
        DISPLAY '{"status":"AUDIT","records":0,"mismatches":0}'
    ELSE
        MOVE "N" TO WS-EOF-FLAG
        PERFORM UNTIL END-OF-FILE
            READ LEDGER-FILE
                AT END
                    SET END-OF-FILE TO TRUE
                NOT AT END
                    PERFORM 3100-CHECK-ROW
            END-READ
        END-PERFORM
        CLOSE LEDGER-FILE
        MOVE WS-TOTAL-RECORDS TO WS-OUT-COUNT
        MOVE WS-BAD-COUNT     TO WS-OUT-BAD
        MOVE WS-OUT-COUNT     TO WS-OUT-COUNT-X
        MOVE WS-OUT-BAD       TO WS-OUT-BAD-X
        DISPLAY '{"status":"AUDIT","records":' FUNCTION TRIM(WS-OUT-COUNT-X)
                ',"mismatches":' FUNCTION TRIM(WS-OUT-BAD-X) '}'
    END-IF.

3100-CHECK-ROW.
    ADD 1 TO WS-TOTAL-RECORDS
    COMPUTE WS-CHECK-TOTAL = REC-QTY * REC-PRICE
    IF WS-CHECK-TOTAL NOT = REC-TOTAL
        ADD 1 TO WS-BAD-COUNT
    END-IF.

9000-SHOW-USAGE.
    DISPLAY "Usage: ledger_book RECORD <tx> <ts> <trader> <sym> <side> <qty> <price>"
    DISPLAY "       ledger_book REPORT"
    DISPLAY "       ledger_book AUDIT"
    MOVE 1 TO RETURN-CODE.
