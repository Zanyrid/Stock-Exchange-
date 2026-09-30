COBC     = cobc
COBFLAGS = -x -free -Wall
TARGET   = bin/ledger_book
SRCS     = ledger_book.cob

all: $(TARGET)

$(TARGET): $(SRCS)
	@mkdir -p bin
	$(COBC) $(COBFLAGS) -o $(TARGET) $(SRCS)

clean:
	rm -rf bin data data_test
