package net.simplehardware.engine.server.services;

import net.simplehardware.engine.server.database.DatabaseManager;
import net.simplehardware.engine.server.database.models.Maze;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating mazes automatically
 */
public class MazeGenerationService {
    private final DatabaseManager db;
    private final String mazeCreatorJarPath;
    private final String mazesDirectory;
    private final ScheduledExecutorService scheduler;

    // Generation parameters
    private static final int EASY_FORMS = 2;
    private static final int EASY_MIN_STEPS = 20;
    private static final int EASY_MAX_STEPS = 25;
    private static final int EASY_SIZE = 10;

    private static final int MEDIUM_FORMS = 3;
    private static final int MEDIUM_MIN_STEPS = 25;
    private static final int MEDIUM_MAX_STEPS = 35;
    private static final int MEDIUM_SIZE = 30;

    private static final int HARD_FORMS = 4;
    private static final int HARD_MIN_STEPS = 40;
    private static final int HARD_MAX_STEPS = 60;
    private static final int HARD_SIZE = 40;

    String[] animal_names = new String[] {
            "Aardvark", "Aardwolf", "Albatross", "Alligator", "Alpaca", "Anaconda", "Angelfish", "Ant", "Anteater",
            "Antelope",
            "Armadillo", "Asp", "Baboon", "Badger", "Bandicoot", "Barnacle", "Barracuda", "Bass", "Bat", "Beaver",
            "Bee", "Beetle", "Bighorn", "Billfish", "Binturong", "Bird", "Bison", "Bittern", "Blackbird", "Bluebird",
            "Boar", "Bobcat", "Bonobo", "Bongo", "Bream", "Buffalo", "Bull", "Bumblebee", "Burbot", "Butterfly",
            "Buzzard", "Camel", "Caracal", "Cardinal", "Caribou", "Carp", "Cat", "Caterpillar", "Catfish", "Cheetah",
            "Chicken", "Chinchilla", "Chipmunk", "Chough", "Cicada", "Clam", "Cobra", "Cod", "Coot", "Cormorant",
            "Cougar", "Cow", "Coyote", "Crab", "Crane", "Crayfish", "Cricket", "Crocodile", "Crow", "Cuckoo",
            "Curlew", "Deer", "Dingo", "Dinosaur", "Dog", "Dolphin", "Donkey", "Dove", "Dragonfly", "Duck",
            "Eagle", "Earthworm", "Echidna", "Eel", "Egret", "Eland", "Elephant", "Elk", "Emu", "Falcon",
            "Ferret", "Finch", "Fish", "Flamingo", "Flea", "Fly", "Fox", "Frog", "Gazelle", "Gecko",
            "Gerbil", "Gibbon", "Gila", "Giraffe", "Gnat", "Gnu", "Goat", "Goldfish", "Goose", "Gopher",
            "Gorilla", "Goshawk", "Grasshopper", "Grebe", "Grouse", "Guanaco", "Gull", "Haddock", "Halibut", "Hamster",
            "Hare", "Hawk", "Hedgehog", "Heron", "Herring", "Hippopotamus", "Hornet", "Horse", "Hummingbird", "Hyena",
            "Ibex", "Ibis", "Iguana", "Impala", "Jackal", "Jaguar", "Jay", "Jellyfish", "Jerboa", "Kangaroo",
            "Kingfisher", "Kite", "Kiwi", "Koala", "Koi", "Krill", "Kudu", "Ladybug", "Lamprey", "Landfowl",
            "Lark", "Lemur", "Leopard", "Limpet", "Lion", "Lizard", "Llama", "Lobster", "Locust", "Loon",
            "Lynx", "Macaw", "Maggot", "Magpie", "Mallard", "Manatee", "Mandrill", "Mantis", "Marlin", "Marmot",
            "Meerkat", "Mink", "Minnow", "Mockingbird", "Mole", "Mollusk", "Mongoose", "Monkey", "Moose", "Mosquito",
            "Moth", "Mouse", "Mule", "Narwhal", "Newt", "Nighthawk", "Nightingale", "Numbat", "Octopus", "Okapi",
            "Opossum", "Orangutan", "Ostrich", "Otter", "Owl", "Ox", "Oyster", "Panda", "Panther", "Parrot",
            "Partridge", "Peacock", "Pelican", "Penguin", "Pheasant", "Pig", "Pigeon", "Pike", "Piranha", "Platypus",
            "Plover", "Polecat", "Porcupine", "Porpoise", "Possum", "Prawn", "Primate", "Ptarmigan", "Puffin", "Puma",
            "Python", "Quail", "Quelea", "Quokka", "Rabbit", "Raccoon", "Ram", "Rat", "Raven", "Reindeer",
            "Reptile", "Rhea", "Rhinoceros", "Roach", "Rodent", "Rook", "Rooster", "Rottweiler", "Salamander", "Salmon",
            "Sandpiper", "Sardine", "Scallop", "Scorpion", "Seagull", "Seahorse", "Seal", "Sealion", "Shark", "Sheep",
            "Shrew", "Shrimp", "Skink", "Skipper", "Skunk", "Sloth", "Slug", "Smelt", "Snail", "Snake",
            "Snipe", "Sole", "Sparrow", "Spider", "Spoonbill", "Squid", "Squirrel", "Starfish", "Stingray", "Stork",
            "Swallow", "Swan", "Swift", "Swordfish", "Tahr", "Takin", "Tapir", "Tarantula", "Tarsier", "Termite",
            "Tern", "Thrush", "Tiger", "Toad", "Tortoise", "Toucan", "Trout", "Turkey", "Turtle", "Viper",
            "Vole", "Vulture", "Wallaby", "Walrus", "Wasp", "Weasel", "Whale", "Whippet", "Whitefish", "Wildcat",
            "Wildebeest", "Wolf", "Wolverine", "Wombat", "Woodcock", "Woodpecker", "Worm", "Wren", "Xerus", "Yak",
            "Zebra", "Zebu", "Alca", "Argali", "Auk", "Avocet", "Boa", "Bok", "Brant", "Caecilian", "Capybara",
            "Cavy", "Char", "Chub", "Chukar", "Civet", "Coati", "Coney", "Cuckold", "Cusk", "Darter",
            "Dassie", "Darter", "Dikdik", "Dorcas", "Dormouse", "Dotterel", "Drupe", "Duiker", "Eelpout", "Fanaloka",
            "Fennec", "Fieldfare", "Fishfly", "Fossa", "Frogfish", "Furrier", "Gadwall", "Gallinule", "Gannet", "Gaura",
            "Gavial", "Genet", "Gerfalcon", "Goby", "Goitred", "Goral", "Goshawk", "Greylag", "Grison", "Gudgeon",
            "Guppy", "Haddock", "Hapuku", "Harrier", "Hartebeest", "Hoatzin", "Huchen", "Hutia", "Ibex", "Ibis",
            "Iguanid", "Ilder", "Inca", "Indri", "Jabiru", "Jackdaw", "Jaeger", "Javelina", "Jird", "Kakapo",
            "Kea", "Kestrel", "Kipuka", "Kite", "Kob", "Kokako", "Kookaburra", "Kowari", "Ksar", "Kudu",
            "Langur", "Lapwing", "Leafbird", "Lechwe", "Liger", "Ling", "Loon", "Lory", "Louse", "Macaque",
            "Madtom", "Magpie", "Mahseer", "Mako", "Manakin", "Margay", "Markhor", "Marten", "Mayfly", "Medaka",
            "Merlin", "Merriam", "Midas", "Moa", "Molly", "Monitor", "Moray", "Murre", "Muskox", "Myna",
            "Nabarlek", "Nandu", "Neddicky", "Needletail", "Nematode", "Nene", "Nicator", "Nigrita", "Nilgai",
            "Noolbenger",
            "Nuthatch", "Olingo", "Onager", "Oribi", "Ortolan", "Oryx", "Ouzel", "Pademelon", "Pangolin", "Pipit",
            "Plover", "Pochard", "Potoroo", "Pratincole", "Puku", "Quoll", "Ratel", "Redstart", "Reedbuck", "Rhebok",
            "Rhinoceros", "Rockfish", "Ronquil", "Saki", "Sambar", "Sard", "Sargo", "Seriema", "Shoveler", "Shrike",
            "Sifaka", "Sika", "Skua", "Slaty", "Smelt", "Snipe", "Solenodon", "Sora", "Spadefish", "Spearfish",
            "Springbok", "Squab", "Squeaker", "Steenbok", "Stilt", "Stint", "Sturgeon", "Sunbird", "Suricate",
            "Tamarin",
            "Tang", "Tapaculo", "Tarpon", "Tench", "Tern", "Thrasher", "Tinamou", "Titmouse", "Topi", "Troupial",
            "Tuatara", "Uakari", "Umbrellabird", "Urchin", "Vaquita", "Vanga", "Vicuna", "Vier", "Vlei", "Vole",
            "Wagtail", "Wahoo", "Wallaro", "Warbler", "Weevil", "Weta", "Widgeon", "Willet", "Wobbegong", "Woodlark",
            "Worm", "Wrasse", "Xenops", "Xerus", "Xiphias", "Xylophilous", "Yak", "Yabby", "Yellowtail", "Yoyo",
            "Zander", "Zebra", "Zebu", "Zonkey", "Zorilla", "Zorro", "Zosterops"
    };

    String[] Alphabet_codes = new String[] {
            "Alfa", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India",
            "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo",
            "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu"
    };

    public MazeGenerationService(DatabaseManager db, String mazeCreatorJarPath, String mazesDirectory) {
        this.db = db;
        this.mazeCreatorJarPath = mazeCreatorJarPath;
        this.mazesDirectory = mazesDirectory;
        this.scheduler = Executors.newScheduledThreadPool(1);

        new File(mazesDirectory).mkdirs();
    }

    /**
     * Start the scheduled maze generation
     * 
     * @param intervalHours How often to generate new mazes (in hours)
     */
    public void startScheduledGeneration(int intervalHours) {
        System.out.println("Starting maze generation service (every " + intervalHours + " hours)");
        generateMazeBatch();

        scheduler.scheduleAtFixedRate(
                this::generateMazeBatch,
                intervalHours,
                intervalHours,
                TimeUnit.HOURS);
    }

    /**
     * Stop the scheduled generation
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Generate a unique maze name using alphabet codes and animal names
     * Format: AlphabetCode_AnimalName (e.g., "Alfa_Elephant", "Bravo_Tiger")
     * 
     * @param difficulty Maze difficulty level
     * @return Unique maze name
     */
    private String generateUniqueMazeName(Maze.Difficulty difficulty) throws Exception {
        Random random = new Random();
        int attempts = 0;
        int maxAttempts = 100;

        do {
            String alphabetCode = Alphabet_codes[random.nextInt(Alphabet_codes.length)];
            String animalName = animal_names[random.nextInt(animal_names.length)];

            final String candidateName = alphabetCode + "_" + animalName + "_" + difficulty.name().toLowerCase();
            List<Maze> existingMazes = db.getActiveMazes();
            boolean nameExists = existingMazes.stream()
                    .anyMatch(m -> m.getName().equals(candidateName));

            if (!nameExists) {
                return candidateName;
            }

            attempts++;
        } while (attempts < maxAttempts);
        return difficulty.name().toLowerCase() + "_maze_" + System.currentTimeMillis();
    }

    /**
     * Generate a batch of mazes (easy, medium, hard)
     */
    public void generateMazeBatch() {
        System.out.println("\n=== Generating new maze batch ===");

        try {
            // Generate 3 easy mazes
            for (int i = 0; i < 3; i++) {
                generateMaze(Maze.Difficulty.EASY, EASY_FORMS, EASY_MIN_STEPS, EASY_MAX_STEPS, EASY_SIZE);
            }

            // Generate 2 medium mazes
            for (int i = 0; i < 2; i++) {
                generateMaze(Maze.Difficulty.MEDIUM, MEDIUM_FORMS, MEDIUM_MIN_STEPS, MEDIUM_MAX_STEPS, MEDIUM_SIZE);
            }

            // Generate 1 hard maze
            generateMaze(Maze.Difficulty.HARD, HARD_FORMS, HARD_MIN_STEPS, HARD_MAX_STEPS, HARD_SIZE);

            System.out.println("=== Maze batch generation complete ===\n");
        } catch (Exception e) {
            System.err.println("Error generating maze batch: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate a single maze
     */
    private void generateMaze(Maze.Difficulty difficulty, int forms, int minSteps, int maxSteps, int size)
            throws Exception {
        Random random = new Random();
        int targetSteps = minSteps + random.nextInt(maxSteps - minSteps + 1);
        String mazeName = generateUniqueMazeName(difficulty);
        String outputPath = mazesDirectory + "/" + mazeName + ".json";

        System.out.println("Generating " + difficulty + " maze: " + mazeName);
        System.out.println("  Forms: " + forms + ", Steps: " + targetSteps + ", Size: " + size + "x" + size);

        // Build command using ProcessBuilder for proper argument handling
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-jar", mazeCreatorJarPath,
                "--generateMaze",
                "--forms", String.valueOf(forms),
                "--prefSteps", String.valueOf(targetSteps),
                "--mazesize", String.valueOf(size),
                "--name", mazeName,
                "--output", outputPath);

        processBuilder.inheritIO();
        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Maze generation failed with exit code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Maze generation interrupted", e);
        }

        File mazeFile = new File(outputPath);
        if (!mazeFile.exists()) {
            throw new IOException("Maze file was not created: " + outputPath);
        }
        Maze maze = db.createMaze(
                mazeName, outputPath, targetSteps, forms, size, difficulty);
        System.out.println("  ✓ Maze created with ID: " + maze.getId() + ", target steps: " + targetSteps);
    }

}
