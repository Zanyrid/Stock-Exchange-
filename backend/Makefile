.PHONY: all setup native compile test clean

all: compile

setup:
	mix deps.get
	$(MAKE) native

native:
	$(MAKE) -C native/fortran
	$(MAKE) -C native/cobol

compile: native
	mix compile

test: compile
	mix test

clean:
	mix clean
	$(MAKE) -C native/fortran clean
	$(MAKE) -C native/cobol clean
