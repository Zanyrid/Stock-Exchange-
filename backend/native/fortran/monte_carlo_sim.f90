! ==============================================================================
! BURSA KOMODITAS ABSURD: SIMULATOR MONTE CARLO & BADAI GEMA
!
! Simulasi Geometric Brownian Motion (GBM) untuk harga komoditas tak berwujud,
! ditambah kejadian acak "Badai Gema" (lonjakan / kejatuhan mendadak).
! Hasil dicetak sebagai satu baris JSON ke stdout (dibaca Elixir lewat Port).
!
! Argumen CLI (semua opsional):
!   --symbol S   --price P   --vol V   --drift D   --steps N
!   --sims M     --horizon H (tahun)   --seed K (0 = acak dari jam)
! ==============================================================================
module absurd_math_mod
    use, intrinsic :: iso_fortran_env, only: dp => real64, i8 => int64
    implicit none
    private
    public :: dp, box_muller, gbm_step, check_echo_storm, sort_ascending
    public :: percentile, init_random_seed, num_str

    real(dp), parameter :: PI = 3.14159265358979323846_dp
    ! Badai Gema: pengali harga = exp(u), u seragam di [-a, a], lalu dinormalkan
    ! supaya rata-rata pengali tepat 1 (tidak menciptakan bias naik/turun).
    real(dp), parameter :: STORM_HALF_RANGE = 0.40_dp
    real(dp), parameter :: STORM_BASE_PROB  = 0.15_dp

contains

    ! Variabel acak normal standar N(0,1) lewat transformasi Box-Muller.
    subroutine box_muller(z0, z1)
        real(dp), intent(out) :: z0, z1
        real(dp) :: u1, u2, r, theta

        call random_number(u1)
        call random_number(u2)
        if (u1 < 1.0e-12_dp) u1 = 1.0e-12_dp   ! hindari log(0)

        r     = sqrt(-2.0_dp * log(u1))
        theta = 2.0_dp * PI * u2
        z0 = r * cos(theta)
        z1 = r * sin(theta)
    end subroutine box_muller

    ! Satu langkah GBM: P' = P * exp((mu - sigma^2/2) dt + sigma sqrt(dt) z)
    function gbm_step(current_p, mu, sigma, dt, z) result(next_p)
        real(dp), intent(in) :: current_p, mu, sigma, dt, z
        real(dp) :: next_p
        real(dp) :: drift_term, diffusion_term

        drift_term     = (mu - 0.5_dp * sigma**2) * dt
        diffusion_term = sigma * sqrt(dt) * z
        next_p = current_p * exp(drift_term + diffusion_term)
    end function gbm_step

    ! Evaluasi kejadian Badai Gema. Peluang naik seiring intensitas (0..1).
    subroutine check_echo_storm(intensity, is_active, multiplier)
        real(dp), intent(in)  :: intensity
        logical,  intent(out) :: is_active
        real(dp), intent(out) :: multiplier
        real(dp) :: dice, roll, u

        call random_number(dice)
        if (dice < STORM_BASE_PROB * intensity) then
            is_active = .true.
            call random_number(roll)
            u = (2.0_dp * roll - 1.0_dp) * STORM_HALF_RANGE
            multiplier = exp(u) * STORM_HALF_RANGE / sinh(STORM_HALF_RANGE)
        else
            is_active = .false.
            multiplier = 1.0_dp
        end if
    end subroutine check_echo_storm

    ! Shell sort menaik (cukup cepat untuk puluhan ribu data).
    subroutine sort_ascending(a)
        real(dp), intent(inout) :: a(:)
        integer :: n, gap, i, j
        real(dp) :: tmp

        n = size(a)
        gap = n / 2
        do while (gap > 0)
            do i = gap + 1, n
                tmp = a(i)
                j = i
                do while (j > gap)
                    if (a(j - gap) <= tmp) exit
                    a(j) = a(j - gap)
                    j = j - gap
                end do
                a(j) = tmp
            end do
            gap = gap / 2
        end do
    end subroutine sort_ascending

    ! Persentil sederhana dari data yang SUDAH terurut (p antara 0 dan 1).
    function percentile(sorted, p) result(val)
        real(dp), intent(in) :: sorted(:)
        real(dp), intent(in) :: p
        real(dp) :: val
        integer :: n, idx

        n = size(sorted)
        idx = max(1, min(n, nint(p * real(n, dp))))
        val = sorted(idx)
    end function percentile

    ! Inisialisasi generator acak. seed = 0 -> pakai jam sistem.
    subroutine init_random_seed(user_seed)
        integer, intent(in) :: user_seed
        integer :: n, k
        integer(i8) :: base, clock_count
        integer, allocatable :: seed_array(:)

        call random_seed(size=n)
        allocate(seed_array(n))

        if (user_seed /= 0) then
            base = int(user_seed, i8)
        else
            call system_clock(clock_count)
            base = clock_count
        end if
        base = mod(abs(base), 1000000007_i8)

        do k = 1, n
            seed_array(k) = int(mod(base * 7919_i8 + int(k, i8) * 104729_i8 + 1_i8, 2147483647_i8))
        end do
        call random_seed(put=seed_array)
        deallocate(seed_array)
    end subroutine init_random_seed

    ! Angka -> teks rapi untuk JSON (tanpa spasi depan, selalu diawali digit).
    function num_str(x) result(s)
        real(dp), intent(in) :: x
        character(len=24) :: s

        write(s, '(F20.4)') max(-1.0e14_dp, min(1.0e14_dp, x))
        s = adjustl(s)
    end function num_str

end module absurd_math_mod

! ------------------------------------------------------------------------------
! PROGRAM UTAMA
! ------------------------------------------------------------------------------
program monte_carlo_sim
    use, intrinsic :: iso_fortran_env, only: output_unit, error_unit
    use absurd_math_mod
    implicit none

    character(len=32) :: symbol
    real(dp) :: base_price, volatility, drift, horizon
    integer  :: steps, num_sims, seed

    real(dp), allocatable :: final_prices(:), sorted_prices(:), sample_path(:)
    real(dp) :: dt, intensity, mean_price, variance, std_dev
    real(dp) :: min_price, max_price, var_95
    real(dp) :: current_p, z0, z1, storm_mult, sample_mult
    logical  :: storm_hit, sample_storm
    integer  :: s, step_idx, storm_count

    ! Nilai bawaan
    symbol     = "GMA.GUA"
    base_price = 100.0_dp
    volatility = 0.35_dp
    drift      = 0.05_dp
    horizon    = 1.0_dp
    steps      = 50
    num_sims   = 100
    seed       = 0
    storm_count = 0
    sample_storm = .false.
    sample_mult  = 1.0_dp

    call parse_cli_args()

    ! Batasi input agar aman
    base_price = max(base_price, 0.01_dp)
    volatility = max(volatility, 0.0001_dp)
    horizon    = max(horizon, 0.0001_dp)
    steps      = max(2, min(steps, 10000))
    num_sims   = max(1, min(num_sims, 200000))

    call init_random_seed(seed)

    allocate(final_prices(num_sims))
    allocate(sorted_prices(num_sims))
    allocate(sample_path(steps))

    dt = horizon / real(steps, dp)
    intensity = min(1.0_dp, volatility * 2.0_dp)

    ! Simulasi ke-1 direkam sebagai "sample path" untuk grafik.
    do s = 1, num_sims
        current_p = base_price
        do step_idx = 1, steps, 2
            call box_muller(z0, z1)
            current_p = gbm_step(current_p, drift, volatility, dt, z0)
            if (s == 1) sample_path(step_idx) = current_p

            if (step_idx + 1 <= steps) then
                current_p = gbm_step(current_p, drift, volatility, dt, z1)
                if (s == 1) sample_path(step_idx + 1) = current_p
            end if
        end do

        ! Badai Gema dicek di akhir setiap lintasan.
        call check_echo_storm(intensity, storm_hit, storm_mult)
        if (storm_hit) storm_count = storm_count + 1
        current_p = current_p * storm_mult

        if (s == 1) then
            sample_storm = storm_hit
            sample_mult  = storm_mult
            sample_path(steps) = current_p
        end if
        final_prices(s) = current_p
    end do

    ! Statistik
    mean_price = sum(final_prices) / real(num_sims, dp)
    variance   = sum((final_prices - mean_price)**2) / real(max(num_sims - 1, 1), dp)
    std_dev    = sqrt(variance)

    sorted_prices = final_prices
    call sort_ascending(sorted_prices)
    min_price = sorted_prices(1)
    max_price = sorted_prices(num_sims)
    var_95    = max(0.0_dp, base_price - percentile(sorted_prices, 0.05_dp))

    call write_json()

    deallocate(final_prices)
    deallocate(sorted_prices)
    deallocate(sample_path)

contains

    subroutine parse_cli_args()
        character(len=64) :: arg, val
        integer :: n, k, ios

        n = command_argument_count()
        k = 1
        do while (k < n)
            call get_command_argument(k, arg)
            call get_command_argument(k + 1, val)
            ios = 0

            select case (trim(arg))
            case ("--symbol")
                symbol = trim(val)
            case ("--price")
                read(val, *, iostat=ios) base_price
            case ("--vol")
                read(val, *, iostat=ios) volatility
            case ("--drift")
                read(val, *, iostat=ios) drift
            case ("--horizon")
                read(val, *, iostat=ios) horizon
            case ("--steps")
                read(val, *, iostat=ios) steps
            case ("--sims")
                read(val, *, iostat=ios) num_sims
            case ("--seed")
                read(val, *, iostat=ios) seed
            case default
                k = k + 1
                cycle
            end select

            if (ios /= 0) then
                write(error_unit, '(A)') 'argumen tidak valid: ' // trim(arg) // ' ' // trim(val)
                stop 2
            end if
            k = k + 2
        end do
    end subroutine parse_cli_args

    subroutine write_json()
        integer :: idx
        character(len=5) :: storm_text

        if (sample_storm) then
            storm_text = 'true'
        else
            storm_text = 'false'
        end if

        write(output_unit, '(A)', advance='no') '{"symbol":"' // trim(symbol) // '",'
        write(output_unit, '(A)', advance='no') '"initial_price":' // trim(num_str(base_price)) // ','
        write(output_unit, '(A)', advance='no') '"simulated_price":' // trim(num_str(mean_price)) // ','
        write(output_unit, '(A)', advance='no') '"sample_price":' // trim(num_str(sample_path(steps))) // ','
        write(output_unit, '(A)', advance='no') '"min_price":' // trim(num_str(min_price)) // ','
        write(output_unit, '(A)', advance='no') '"max_price":' // trim(num_str(max_price)) // ','
        write(output_unit, '(A)', advance='no') '"std_deviation":' // trim(num_str(std_dev)) // ','
        write(output_unit, '(A)', advance='no') '"var_95":' // trim(num_str(var_95)) // ','
        write(output_unit, '(A)', advance='no') '"echo_storm":' // trim(storm_text) // ','
        write(output_unit, '(A)', advance='no') '"storm_multiplier":' // trim(num_str(sample_mult)) // ','
        write(output_unit, '(A)', advance='no') '"storm_frequency":' // &
            trim(num_str(real(storm_count, dp) / real(num_sims, dp))) // ','
        write(output_unit, '(A)', advance='no') '"path":['
        do idx = 1, steps
            if (idx > 1) write(output_unit, '(A)', advance='no') ','
            write(output_unit, '(A)', advance='no') trim(num_str(sample_path(idx)))
        end do
        write(output_unit, '(A)') ']}'
    end subroutine write_json

end program monte_carlo_sim
