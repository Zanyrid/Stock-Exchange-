# Tes yang butuh biner Fortran/COBOL diberi tag :native dan dilewati
# otomatis bila biner belum dikompilasi (jalankan `make native` dulu).
native? =
  File.exists?(Application.fetch_env!(:bursa_absurd, :fortran_bin)) and
    File.exists?(Application.fetch_env!(:bursa_absurd, :cobol_bin))

ExUnit.start(exclude: if(native?, do: [], else: [:native]))
