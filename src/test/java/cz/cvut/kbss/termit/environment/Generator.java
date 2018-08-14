package cz.cvut.kbss.termit.environment;

import cz.cvut.kbss.termit.model.User;
import cz.cvut.kbss.termit.util.Vocabulary;

import java.net.URI;
import java.util.Collection;
import java.util.Random;

public class Generator {

    private static Random random = new Random();

    private Generator() {
        throw new AssertionError();
    }

    /**
     * Generates a (pseudo) random URI, usable for test individuals.
     *
     * @return Random URI
     */
    public static URI generateUri() {
        return URI.create(Vocabulary.ONTOLOGY_IRI_termit + "/randomInstance" + randomInt());
    }

    /**
     * Generates a (pseudo-)random integer between the specified lower and upper bounds.
     *
     * @param lowerBound Lower bound, inclusive
     * @param upperBound Upper bound, exclusive
     * @return Randomly generated integer
     */
    public static int randomInt(int lowerBound, int upperBound) {
        int rand;
        do {
            rand = random.nextInt(upperBound);
        } while (rand < lowerBound);
        return rand;
    }

    /**
     * Generates a (pseudo) random integer.
     * <p>
     * This version has no bounds (aside from the integer range), so the returned number may be negative or zero.
     *
     * @return Randomly generated integer
     * @see #randomInt(int, int)
     */
    public static int randomInt() {
        return random.nextInt();
    }

    /**
     * Generates a (pseudo)random index of an element in the collection.
     * <p>
     * I.e. the returned number is in the interval <0, col.size()).
     *
     * @param col The collection
     * @return Random index
     */
    public static int randomIndex(Collection<?> col) {
        assert col != null;
        assert !col.isEmpty();
        return random.nextInt(col.size());
    }

    /**
     * Generators a (pseudo) random boolean.
     *
     * @return Random boolean
     */
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    /**
     * Creates a random instance of {@link User}.
     * <p>
     * The instance has no identifier set.
     *
     * @return New {@code User} instance
     * @see #generateUserWithId()
     */
    public static User generateUser() {
        final User user = new User();
        user.setFirstName("Firstname" + randomInt());
        user.setLastName("Lastname" + randomInt());
        user.setUsername("user" + randomInt() + "@kbss.felk.cvut.cz");
        user.setPassword(Integer.toString(randomInt()));
        return user;
    }

    /**
     * Creates a random instance of {@link User} with a generated identifier.
     * <p>
     * The presence of identifier is the only difference between this method and {@link #generateUser()}.
     *
     * @return New {@code User} instance
     */
    public static User generateUserWithId() {
        final User user = generateUser();
        user.setUri(Generator.generateUri());
        return user;
    }
}