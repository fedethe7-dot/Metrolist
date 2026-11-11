/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.beat

/**
 * A music genre Beat knows how to build mixes for.
 *
 * [artists] are signature artists (lowercase) used to recognise the genre in the listening history;
 * [seeds] are searches that lead to a typical song, used to start YouTube Music radios.
 * [neighbors] are compatible genres, used to suggest music outside the usual habits without going random.
 */
data class BeatGenre(
    val id: String,
    val name: String,
    val artists: Set<String>,
    val seeds: List<String>,
    val colors: Pair<Long, Long>,
    val neighbors: List<String>,
    val original: Boolean = false,
)

object BeatMixCatalog {
    val genres: List<BeatGenre> =
        listOf(
            // The user's original mixes: always available in "I miei mix".
            BeatGenre(
                id = "acid_jazz",
                name = "Acid jazz",
                artists = setOf(
                    "jamiroquai", "the brand new heavies", "brand new heavies", "incognito", "us3", "galliano",
                    "corduroy", "james taylor quartet", "the james taylor quartet", "d'influence", "young disciples",
                    "omar", "matt bianco", "swing out sister", "sade", "mother earth", "down to the bone",
                ),
                seeds = listOf(
                    "Jamiroquai Virtual Insanity", "Jamiroquai Cosmic Girl", "The Brand New Heavies Never Stop",
                    "Incognito Still a Friend of Mine", "US3 Cantaloop", "Galliano Long Time Gone",
                    "Jamiroquai Space Cowboy", "Down to the Bone Staten Island Groove",
                ),
                colors = 0xFFFF8A00 to 0xFF8E2DE2,
                neighbors = listOf("funk_soul", "nu_disco", "jazz", "rnb"),
                original = true,
            ),
            BeatGenre(
                id = "anni80",
                name = "Anni '80",
                artists = setOf(
                    "a-ha", "wham!", "wham", "duran duran", "cyndi lauper", "madonna", "michael jackson", "george michael",
                    "tears for fears", "depeche mode", "spandau ballet", "culture club", "eurythmics", "the human league",
                    "simple minds", "pet shop boys", "kim wilde", "rick astley", "prince", "whitney houston",
                    "lionel richie", "phil collins", "toto", "new order", "the police", "talk talk", "alphaville",
                ),
                seeds = listOf(
                    "a-ha Take On Me", "Wham Wake Me Up Before You Go-Go", "Duran Duran Rio",
                    "Cyndi Lauper Girls Just Want to Have Fun", "Tears for Fears Everybody Wants to Rule the World",
                    "Rick Astley Never Gonna Give You Up", "Alphaville Forever Young", "Simple Minds Dont You Forget About Me",
                ),
                colors = 0xFFFF2E93 to 0xFF00D4FF,
                neighbors = listOf("synthwave", "italo_disco", "anni70", "rock_classico"),
                original = true,
            ),
            BeatGenre(
                id = "anni80_rock",
                name = "Anni '80 action e rock",
                artists = setOf(
                    "survivor", "europe", "bon jovi", "kenny loggins", "van halen", "def leppard", "journey", "foreigner",
                    "scorpions", "guns n' roses", "john parr", "joan jett & the blackhearts", "joan jett", "pat benatar",
                    "bryan adams", "dire straits", "starship", "bonnie tyler", "billy idol", "whitesnake",
                ),
                seeds = listOf(
                    "Survivor Eye of the Tiger", "Europe The Final Countdown", "Bon Jovi Livin on a Prayer",
                    "Kenny Loggins Danger Zone", "John Parr St. Elmos Fire", "Bonnie Tyler Holding Out for a Hero",
                    "Journey Dont Stop Believin", "Van Halen Jump",
                ),
                colors = 0xFFE53935 to 0xFF212121,
                neighbors = listOf("rock_classico", "anni80", "synthwave"),
                original = true,
            ),
            BeatGenre(
                id = "anni70",
                name = "Anni '70",
                artists = setOf(
                    "bee gees", "abba", "earth, wind & fire", "earth wind & fire", "boney m.", "boney m",
                    "kc and the sunshine band", "gloria gaynor", "donna summer", "chic", "the jackson 5", "jackson 5",
                    "barry white", "village people", "the trammps", "electric light orchestra", "elton john", "sister sledge",
                ),
                seeds = listOf(
                    "Bee Gees Stayin Alive", "ABBA Dancing Queen", "Earth Wind and Fire September", "Boney M Rasputin",
                    "Gloria Gaynor I Will Survive", "Donna Summer Hot Stuff", "Sister Sledge We Are Family",
                    "Electric Light Orchestra Mr. Blue Sky",
                ),
                colors = 0xFFF9A825 to 0xFFD84315,
                neighbors = listOf("funk_soul", "house_disco", "anni80", "rock_classico"),
                original = true,
            ),
            BeatGenre(
                id = "anni2000",
                name = "Anni 2000",
                artists = setOf(
                    "outkast", "black eyed peas", "the black eyed peas", "nelly furtado", "beyoncé", "beyonce", "rihanna",
                    "gwen stefani", "justin timberlake", "the killers", "gorillaz", "maroon 5", "christina aguilera",
                    "kelly clarkson", "avril lavigne", "coldplay", "nelly", "sean paul", "timbaland",
                ),
                seeds = listOf(
                    "Outkast Hey Ya", "Black Eyed Peas I Gotta Feeling", "Shakira Hips Dont Lie", "Eminem Without Me",
                    "Nelly Furtado Promiscuous", "Gorillaz Feel Good Inc", "Sean Paul Temperature", "Justin Timberlake SexyBack",
                ),
                colors = 0xFF00C853 to 0xFF1565C0,
                neighbors = listOf("hiphop", "rnb", "latin_pop", "indie"),
                original = true,
            ),
            BeatGenre(
                id = "house_disco",
                name = "House e disco",
                artists = setOf(
                    "daft punk", "stardust", "modjo", "purple disco machine", "cassius", "bob sinclar", "roger sanchez",
                    "spiller", "the shapeshifters", "armand van helden", "kungs", "sophie ellis-bextor", "fisher",
                    "storm queen", "the martinez brothers", "mousse t.", "eric prydz",
                ),
                seeds = listOf(
                    "Daft Punk One More Time", "Stardust Music Sounds Better With You", "Modjo Lady Hear Me Tonight",
                    "Purple Disco Machine Hypnotized", "Spiller Groovejet", "Roger Sanchez Another Chance",
                    "Storm Queen Look Right Through", "Cassius Feeling for You",
                ),
                colors = 0xFF7C4DFF to 0xFFFF4081,
                neighbors = listOf("nu_disco", "elettronica", "dance90", "funk_soul"),
                original = true,
            ),
            // Genres Beat can propose on its own, from the listening history.
            BeatGenre(
                id = "reggaeton",
                name = "Reggaeton",
                artists = setOf(
                    "bad bunny", "j balvin", "daddy yankee", "karol g", "ozuna", "anuel aa", "rauw alejandro", "feid",
                    "maluma", "nicky jam", "don omar", "wisin & yandel", "wisin y yandel", "myke towers", "sech",
                    "farruko", "jhayco", "quevedo", "mora", "young miko", "manuel turizo", "tainy",
                ),
                seeds = listOf(
                    "Daddy Yankee Gasolina", "Bad Bunny Titi Me Pregunto", "J Balvin Mi Gente", "Karol G Provenza",
                    "Don Omar Danza Kuduro", "Rauw Alejandro Todo de Ti", "Feid Classy 101", "Ozuna Baila Baila Baila",
                ),
                colors = 0xFFFF1744 to 0xFFFFC400,
                neighbors = listOf("latin_pop", "afrobeats", "rap_italiano", "anni2000"),
            ),
            BeatGenre(
                id = "latin_pop",
                name = "Latin pop",
                artists = setOf(
                    "shakira", "enrique iglesias", "ricky martin", "luis fonsi", "marc anthony", "camila cabello", "rosalía",
                    "rosalia", "sebastián yatra", "sebastian yatra", "carlos vives", "juanes", "gente de zona", "c. tangana",
                    "alvaro soler", "pitbull",
                ),
                seeds = listOf(
                    "Luis Fonsi Despacito", "Enrique Iglesias Bailando", "Ricky Martin Livin la Vida Loca", "Rosalia Despecha",
                    "Carlos Vives La Bicicleta", "Alvaro Soler Sofia", "Marc Anthony Vivir Mi Vida",
                ),
                colors = 0xFFFF6D00 to 0xFFD500F9,
                neighbors = listOf("reggaeton", "anni2000", "pop_italiano", "afrobeats"),
            ),
            BeatGenre(
                id = "hiphop",
                name = "Hip hop",
                artists = setOf(
                    "eminem", "dr. dre", "50 cent", "snoop dogg", "kendrick lamar", "drake", "kanye west", "jay-z",
                    "the notorious b.i.g.", "2pac", "nas", "a tribe called quest", "missy elliott", "travis scott", "j. cole",
                    "the pharcyde", "de la soul", "coolio",
                ),
                seeds = listOf(
                    "Dr. Dre Still D.R.E.", "50 Cent In Da Club", "Kendrick Lamar HUMBLE.", "A Tribe Called Quest Can I Kick It",
                    "Snoop Dogg Drop It Like Its Hot", "Coolio Gangstas Paradise", "The Pharcyde Passin Me By",
                ),
                colors = 0xFF212121 to 0xFFFFD600,
                neighbors = listOf("rnb", "rap_italiano", "funk_soul", "anni2000"),
            ),
            BeatGenre(
                id = "rnb",
                name = "R&B",
                artists = setOf(
                    "usher", "alicia keys", "the weeknd", "bruno mars", "h.e.r.", "sza", "frank ocean", "destiny's child",
                    "tlc", "aaliyah", "ne-yo", "mary j. blige", "anderson .paak", "silk sonic", "daniel caesar", "brandy",
                ),
                seeds = listOf(
                    "Silk Sonic Leave the Door Open", "Usher Yeah", "Alicia Keys Fallin", "The Weeknd Blinding Lights",
                    "TLC No Scrubs", "Bruno Mars Treasure", "Daniel Caesar Best Part",
                ),
                colors = 0xFF6A1B9A to 0xFFEC407A,
                neighbors = listOf("funk_soul", "hiphop", "acid_jazz", "afrobeats"),
            ),
            BeatGenre(
                id = "funk_soul",
                name = "Funk e soul",
                artists = setOf(
                    "james brown", "stevie wonder", "marvin gaye", "aretha franklin", "kool & the gang", "kool and the gang",
                    "rick james", "parliament", "curtis mayfield", "al green", "vulfpeck", "cory wong", "the meters",
                    "sly & the family stone", "chaka khan", "the isley brothers", "lettuce", "nile rodgers",
                ),
                seeds = listOf(
                    "James Brown I Got You", "Stevie Wonder Superstition", "Kool and the Gang Celebration",
                    "Marvin Gaye Got to Give It Up", "Vulfpeck Dean Town", "Chaka Khan Aint Nobody", "Rick James Super Freak",
                ),
                colors = 0xFFFF6F00 to 0xFF4E342E,
                neighbors = listOf("acid_jazz", "anni70", "nu_disco", "rnb"),
            ),
            BeatGenre(
                id = "nu_disco",
                name = "Nu disco e funky house",
                artists = setOf(
                    "dua lipa", "jungle", "parcels", "roosevelt", "breakbot", "chromeo", "l'impératrice", "todd terje",
                    "crazy p", "folamour", "lindstrøm", "kraak & smaak", "the knocks", "dimitri from paris",
                    "purple disco machine", "kungs",
                ),
                seeds = listOf(
                    "Purple Disco Machine Dished", "Jungle Back On 74", "Parcels Tieduprightnow", "Breakbot Baby Im Yours",
                    "Dua Lipa Levitating", "LImperatrice Agitations tropicales", "Roosevelt Under the Sun",
                ),
                colors = 0xFF00E5FF to 0xFFFF4081,
                neighbors = listOf("house_disco", "funk_soul", "acid_jazz", "synthwave"),
            ),
            BeatGenre(
                id = "synthwave",
                name = "Synthwave e retro",
                artists = setOf(
                    "the midnight", "kavinsky", "gunship", "fm-84", "timecop1983", "perturbator", "carpenter brut",
                    "college", "electric youth", "lazerhawk", "mitch murder", "michael oakley", "le cassette",
                ),
                seeds = listOf(
                    "Kavinsky Nightcall", "The Midnight Los Angeles", "FM-84 Running in the Night", "College A Real Hero",
                    "Gunship Tech Noir", "Michael Oakley Turn Back Time",
                ),
                colors = 0xFFD500F9 to 0xFF00B0FF,
                neighbors = listOf("anni80", "anni80_rock", "elettronica", "italo_disco"),
            ),
            BeatGenre(
                id = "rock_classico",
                name = "Rock classico",
                artists = setOf(
                    "queen", "ac/dc", "led zeppelin", "the rolling stones", "aerosmith", "deep purple", "pink floyd", "the who",
                    "lynyrd skynyrd", "eagles", "fleetwood mac", "creedence clearwater revival", "kiss", "boston",
                    "the doors", "dire straits", "the beatles",
                ),
                seeds = listOf(
                    "Queen Dont Stop Me Now", "AC/DC Highway to Hell", "Boston More Than a Feeling", "Aerosmith Dream On",
                    "Toto Hold the Line", "Fleetwood Mac Go Your Own Way", "Creedence Clearwater Revival Fortunate Son",
                ),
                colors = 0xFFB71C1C to 0xFF37474F,
                neighbors = listOf("anni80_rock", "anni70", "indie", "cantautori"),
            ),
            BeatGenre(
                id = "indie",
                name = "Indie e alternative",
                artists = setOf(
                    "arctic monkeys", "franz ferdinand", "tame impala", "the strokes", "phoenix", "mgmt",
                    "foster the people", "two door cinema club", "the black keys", "kings of leon", "glass animals",
                    "florence + the machine", "vampire weekend", "the killers", "kasabian", "empire of the sun",
                ),
                seeds = listOf(
                    "The Killers Mr. Brightside", "Arctic Monkeys Do I Wanna Know", "Phoenix 1901", "MGMT Electric Feel",
                    "Two Door Cinema Club What You Know", "Tame Impala The Less I Know the Better", "Glass Animals Heat Waves",
                ),
                colors = 0xFF2E7D32 to 0xFFFFAB00,
                neighbors = listOf("rock_classico", "anni2000", "nu_disco", "synthwave"),
            ),
            BeatGenre(
                id = "pop_italiano",
                name = "Pop italiano",
                artists = setOf(
                    "eros ramazzotti", "laura pausini", "tiziano ferro", "jovanotti", "cesare cremonini", "mahmood",
                    "måneskin", "maneskin", "elisa", "marco mengoni", "ultimo", "annalisa", "umberto tozzi", "raf",
                    "gianna nannini", "883", "max pezzali", "elodie", "angelina mango", "the kolors",
                ),
                seeds = listOf(
                    "Max Pezzali Gli anni", "Jovanotti Penso positivo", "Cesare Cremonini Poetica", "Annalisa Mon Amour",
                    "Umberto Tozzi Gloria", "883 Come mai", "The Kolors Italodisco",
                ),
                colors = 0xFF009246 to 0xFFCE2B37,
                neighbors = listOf("cantautori", "italo_disco", "latin_pop", "rap_italiano"),
            ),
            BeatGenre(
                id = "rap_italiano",
                name = "Rap italiano",
                artists = setOf(
                    "sfera ebbasta", "marracash", "guè", "gue", "lazza", "salmo", "fabri fibra", "club dogo", "articolo 31",
                    "j-ax", "geolier", "anna", "tedua", "ghali", "mr. rain", "noyz narcos", "frankie hi-nrg mc",
                ),
                seeds = listOf(
                    "Articolo 31 Domani smetto", "Club Dogo Weekend", "Marracash Body Parts", "Geolier I p me tu p te",
                    "Sfera Ebbasta Tran Tran", "Frankie hi-nrg mc Quelli che benpensano",
                ),
                colors = 0xFF263238 to 0xFF00E676,
                neighbors = listOf("hiphop", "pop_italiano", "reggaeton"),
            ),
            BeatGenre(
                id = "italo_disco",
                name = "Italo disco",
                artists = setOf(
                    "gazebo", "righeira", "sabrina", "den harrow", "ryan paris", "savage", "baltimora", "spagna",
                    "fun fun", "kano", "giorgio moroder", "p. lion", "joe yellow", "valerie dore", "scotch",
                ),
                seeds = listOf(
                    "Gazebo I Like Chopin", "Ryan Paris Dolce Vita", "Righeira Vamos a la playa", "Baltimora Tarzan Boy",
                    "Savage Dont Cry Tonight", "Scotch Disco Band", "Giorgio Moroder Chase",
                ),
                colors = 0xFFFF4081 to 0xFF651FFF,
                neighbors = listOf("anni80", "house_disco", "synthwave", "pop_italiano"),
            ),
            BeatGenre(
                id = "dance90",
                name = "Dance anni '90",
                artists = setOf(
                    "corona", "haddaway", "ice mc", "dr. alban", "2 unlimited", "snap!", "culture beat", "gala",
                    "eiffel 65", "alexia", "the outhere brothers", "la bouche", "ace of base", "robin s", "black box",
                    "robert miles", "datura", "whigfield",
                ),
                seeds = listOf(
                    "Corona The Rhythm of the Night", "Haddaway What Is Love", "Gala Freed from Desire", "Eiffel 65 Blue",
                    "Black Box Ride on Time", "Robert Miles Children", "Whigfield Saturday Night",
                ),
                colors = 0xFF00E676 to 0xFF2979FF,
                neighbors = listOf("house_disco", "italo_disco", "anni2000", "elettronica"),
            ),
            BeatGenre(
                id = "elettronica",
                name = "Elettronica",
                artists = setOf(
                    "fred again..", "disclosure", "bonobo", "moderat", "rüfüs du sol", "rufus du sol", "kaytranada",
                    "caribou", "odesza", "four tet", "the chemical brothers", "fatboy slim", "the prodigy", "justice",
                    "röyksopp", "royksopp", "moby", "massive attack",
                ),
                seeds = listOf(
                    "Disclosure Latch", "Fred again Delilah", "Kaytranada 10%", "Justice D.A.N.C.E.", "Fatboy Slim Praise You",
                    "Moby Porcelain", "Rufus Du Sol Innerbloom",
                ),
                colors = 0xFF1DE9B6 to 0xFF311B92,
                neighbors = listOf("house_disco", "nu_disco", "synthwave", "dance90"),
            ),
            BeatGenre(
                id = "afrobeats",
                name = "Afrobeats",
                artists = setOf(
                    "burna boy", "wizkid", "rema", "tems", "davido", "ckay", "ayra starr", "fireboy dml", "asake", "tyla",
                    "joeboy", "omah lay",
                ),
                seeds = listOf(
                    "Rema Calm Down", "Burna Boy Last Last", "Wizkid Essence", "CKay Love Nwantiti", "Tyla Water",
                    "Ayra Starr Rush",
                ),
                colors = 0xFFFFAB00 to 0xFF00C853,
                neighbors = listOf("reggaeton", "rnb", "latin_pop"),
            ),
            BeatGenre(
                id = "jazz",
                name = "Jazz e lounge",
                artists = setOf(
                    "miles davis", "chet baker", "herbie hancock", "george benson", "norah jones", "michael bublé",
                    "michael buble", "diana krall", "gregory porter", "kamasi washington", "robert glasper", "st germain",
                    "nicola conte", "dave brubeck", "the dave brubeck quartet",
                ),
                seeds = listOf(
                    "Herbie Hancock Cantaloupe Island", "George Benson Give Me the Night", "St Germain Rose Rouge",
                    "Gregory Porter Liquid Spirit", "Chet Baker Almost Blue", "Dave Brubeck Take Five",
                ),
                colors = 0xFF3E2723 to 0xFFFFB300,
                neighbors = listOf("acid_jazz", "funk_soul", "rnb", "cantautori"),
            ),
            BeatGenre(
                id = "cantautori",
                name = "Cantautori italiani",
                artists = setOf(
                    "lucio battisti", "lucio dalla", "franco battiato", "fabrizio de andré", "pino daniele",
                    "francesco de gregori", "rino gaetano", "paolo conte", "vasco rossi", "zucchero", "ivano fossati",
                    "edoardo bennato", "francesco guccini", "adriano celentano",
                ),
                seeds = listOf(
                    "Lucio Battisti Emozioni", "Pino Daniele Je so pazzo", "Rino Gaetano Ma il cielo e sempre piu blu",
                    "Lucio Dalla Futura", "Franco Battiato Centro di gravita permanente", "Paolo Conte Azzurro",
                ),
                colors = 0xFF1565C0 to 0xFF8D6E63,
                neighbors = listOf("pop_italiano", "jazz", "rock_classico"),
            ),
        )

    private val byId = genres.associateBy { it.id }

    fun genre(id: String?): BeatGenre? = id?.let { byId[it] }

    val originals: List<BeatGenre> get() = genres.filter { it.original }

    /** Genres an artist (lowercase name) belongs to according to the catalog. */
    fun genresOfArtist(normalizedName: String): List<String> =
        genres.filter { normalizedName in it.artists }.map { it.id }
}

/** Lowercase, trimmed artist name used as a key everywhere in the mix engine. */
fun normalizeArtist(name: String): String = name.lowercase().trim()
