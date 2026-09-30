FC      = gfortran
FFLAGS  = -O2 -Wall -Wextra -std=f2008
TARGET  = bin/monte_carlo_sim
SRCS    = monte_carlo_sim.f90

all: $(TARGET)

$(TARGET): $(SRCS)
	@mkdir -p bin
	$(FC) $(FFLAGS) -o $(TARGET) $(SRCS)

clean:
	rm -rf bin
