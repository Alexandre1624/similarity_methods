public class BoucleTest {
    public static void main(String[] args) throws Exception {
        long DUREE = 5_000_000_000L; // 5 secondes
        long debut = System.nanoTime();
        long tempsIgnore = 0;
        int count = 0;
        String[] files = {"A", "B", "C"};

        for (int i = 0; ; i++) {
            if (i >= files.length) i = 0;

            // PARTIE À NE PAS COMPTER
            long debutIgnorer = System.nanoTime();
            Thread.sleep(20); // Simule parseGraph + PageRank
            long finIgnorer = System.nanoTime();
            long dureeIgnorer = finIgnorer - debutIgnorer;
            tempsIgnore += dureeIgnorer;

            // PARTIE À COMPTER
            Thread.sleep(10); // Simule func.compute (partie à compter)

            long tempsEcoule = System.nanoTime() - debut - tempsIgnore;

            System.out.println("Itération: " + count +
                    " | tempsEcoule=" + (tempsEcoule / 1_000_000) +
                    " ms | tempsIgnore=" + (tempsIgnore / 1_000_000) +
                    " ms | now=" + System.nanoTime());

            if (tempsEcoule > DUREE) break;
            count++;
        }
        System.out.println("Fini après " + count + " itérations");
    }
}
