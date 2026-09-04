package com.example.gadgetmover.model.filter

/**
 * The mechanical keyboard switch catalogue behind [FilterType.SwitchSystemBuilder] — every model
 * label is already brand-prefixed (e.g. "Cherry MX2A Black"), so its slug is unique across brands
 * without needing a composite key. No "Other" entries: [FilterType.SearchablePopupSelect]'s
 * `allowCustomInput` already covers anything not in this list, so a redundant catalogue entry
 * would just duplicate that.
 */
object SwitchCatalog {
    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val brands: List<FilterOption> = options(
        "Cherry", "Gateron", "Kailh", "TTC", "Outemu", "Akko", "Haimu", "JWK / Durock",
        "Everglide", "KTT", "Tecsee", "Gazzew", "Zeal", "Glorious", "NovelKeys", "Keychron",
        "Razer", "Logitech", "SteelSeries", "Corsair", "Wooting / Lekker", "NuPhy",
        "Topre / Electro-Capacitive", "Alps / Matias", "Other Magnetic Switches"
    )

    private val modelsByBrand: Map<String, List<FilterOption>> = mapOf(
        slug("Cherry") to options(
            "Cherry MX2A Black", "Cherry MX2A Red", "Cherry MX2A Brown", "Cherry MX2A Blue",
            "Cherry MX2A Silent Red", "Cherry MX2A Silent Black", "Cherry MX2A Speed Silver",
            "Cherry MX2A Ergo Clear", "Cherry MX2A Black Clear-Top", "Cherry MX2A Orange",
            "Cherry MX2A Purple", "Cherry MX2A Honey", "Cherry MX Blossom", "Cherry MX Falcon",
            "Cherry MX Petal", "Cherry MX Northern Light", "Cherry MX Green", "Cherry MX Clear",
            "Cherry MX Grey Linear", "Cherry MX Grey Tactile", "Cherry MX White",
            "Cherry MX Low Profile Red", "Cherry MX Low Profile Speed",
            "Cherry MX Low Profile 2.0 Red", "Cherry MX Low Profile 2.0 Speed",
            "Cherry MX Ultra Low Profile Tactile", "Cherry MX Ultra Low Profile Click",
            "Cherry MX Multipoint Silver"
        ),
        slug("Gateron") to options(
            "Gateron KS-3 Red", "Gateron KS-3 Yellow", "Gateron KS-3 Black", "Gateron KS-3 Brown",
            "Gateron KS-3 Blue", "Gateron KS-3 Green", "Gateron KS-3 Clear",
            "Gateron KS-8 Red", "Gateron KS-8 Yellow", "Gateron KS-8 Black", "Gateron KS-8 Brown", "Gateron KS-8 Blue",
            "Gateron KS-9 Red", "Gateron KS-9 Yellow", "Gateron KS-9 Black", "Gateron KS-9 Brown", "Gateron KS-9 Blue",
            "Gateron Milky Red", "Gateron Milky Yellow", "Gateron Milky Black", "Gateron Milky Brown", "Gateron Milky Blue",
            "Gateron Milky Yellow Pro",
            "Gateron G Pro 2.0 Red", "Gateron G Pro 2.0 Yellow", "Gateron G Pro 2.0 Black", "Gateron G Pro 2.0 Brown",
            "Gateron G Pro 2.0 Blue", "Gateron G Pro 2.0 Silver",
            "Gateron G Pro 3.0 Red", "Gateron G Pro 3.0 Yellow", "Gateron G Pro 3.0 Black", "Gateron G Pro 3.0 Brown",
            "Gateron G Pro 3.0 Blue", "Gateron G Pro 3.0 Silver",
            "Gateron Ink Black V2", "Gateron Ink Red V2", "Gateron Ink Yellow V2", "Gateron Ink Blue V2",
            "Gateron Silent Ink Black V2", "Gateron Box Ink Black", "Gateron Box Ink Pink",
            "Gateron Oil King", "Gateron North Pole", "Gateron North Pole 2.0", "Gateron CJ",
            "Gateron Cap Golden Yellow V2", "Gateron Cap Milky Yellow V2", "Gateron Cap V2 Brown",
            "Gateron Baby Kangaroo", "Gateron Baby Kangaroo 2.0", "Gateron Quinn", "Gateron Beer",
            "Gateron Mini i", "Gateron Melodic", "Gateron Smoothie", "Gateron Smoothie Silver",
            "Gateron Lunar Probe", "Gateron Cream Soda", "Gateron Mountain Top", "Gateron Azure Dragon V2",
            "Gateron Luciola", "Gateron Root Beer Float", "Gateron Baby Raccoon", "Gateron Mini M",
            "Gateron Type L", "Gateron Jupiter Red", "Gateron Jupiter Brown", "Gateron Jupiter Banana",
            "Gateron Silent Red", "Gateron Silent Black", "Gateron Silent Brown",
            "Gateron Aliaz Silent 60g", "Gateron Aliaz Silent 70g", "Gateron Aliaz Silent 80g", "Gateron Aliaz Silent 100g",
            "Gateron Low Profile Red", "Gateron Low Profile Brown", "Gateron Low Profile Blue",
            "Gateron Low Profile 2.0 Red", "Gateron Low Profile 2.0 Brown", "Gateron Low Profile 2.0 Blue",
            "Gateron Low Profile 2.0 Banana", "Gateron Low Profile 2.0 Silver",
            "Gateron Low Profile 3.0 Red", "Gateron Low Profile 3.0 Brown", "Gateron Low Profile 3.0 Silver",
            "Gateron Optical Red", "Gateron Optical Yellow", "Gateron Optical Black", "Gateron Optical Brown",
            "Gateron Optical Blue", "Gateron Optical Silver", "Gateron Optical Green", "Gateron Optical Clear",
            "Gateron Optical Silent Red",
            "Gateron KS-20 Magnetic White", "Gateron KS-20 Magnetic Orange",
            "Gateron KS-20U Dual-Rail Magnetic White", "Gateron KS-20U Dual-Rail Magnetic Jade",
            "Gateron Magnetic Jade", "Gateron Magnetic Jade Pro", "Gateron Magnetic Jade Max",
            "Gateron Magnetic Jade Gaming", "Gateron Magnetic Jade Mini E",
            "Gateron Magnetic Nebula", "Gateron Magnetic Dawn", "Gateron Magnetic Aurora",
            "Gateron Magnetic Zero Degree", "Gateron Magnetic Double-Rail Lunar Probe"
        ),
        slug("Kailh") to options(
            "Kailh Red", "Kailh Black", "Kailh Brown", "Kailh Blue",
            "Kailh Speed Silver", "Kailh Speed Copper", "Kailh Speed Bronze", "Kailh Speed Gold",
            "Kailh Pro Burgundy", "Kailh Pro Purple", "Kailh Pro Light Green", "Kailh Cream",
            "Kailh Super Speed Red", "Kailh Super Speed Silver", "Kailh Super Speed Copper", "Kailh Super Speed Bronze",
            "Kailh BOX Red", "Kailh BOX Black", "Kailh BOX Yellow", "Kailh BOX Brown", "Kailh BOX White",
            "Kailh BOX Pale Blue", "Kailh BOX Dark Yellow", "Kailh BOX Burnt Orange", "Kailh BOX Navy",
            "Kailh BOX Jade", "Kailh BOX Pink", "Kailh BOX Royal", "Kailh BOX Ancient Grey", "Kailh BOX Chinese Red",
            "Kailh BOX Silent Pink", "Kailh BOX Silent Brown",
            "Kailh BOX Crystal Pink", "Kailh BOX Crystal Jade", "Kailh BOX Crystal Navy", "Kailh BOX Crystal Royal",
            "Kailh BOX V2 Red", "Kailh BOX V2 Brown", "Kailh BOX V2 White",
            "Kailh BOX Winter", "Kailh BOX Summer", "Kailh BOX Autumn", "Kailh BOX Spring",
            "Kailh BOX Deep Sea Silent Pro Islet", "Kailh BOX Deep Sea Silent Pro Whale",
            "Kailh BOX Deep Sea Silent Mini Islet", "Kailh BOX Deep Sea Silent Mini Whale",
            "Kailh Midnight Pro Silent", "Kailh Clione Limacina", "Kailh Clione Limacina Light",
            "Kailh Canary", "Kailh Polia", "Kailh Fried Egg", "Kailh Hush Silent",
            "Kailh Choc Red", "Kailh Choc Black", "Kailh Choc Brown", "Kailh Choc White",
            "Kailh Choc Pale Blue", "Kailh Choc Burnt Orange", "Kailh Choc Dark Yellow",
            "Kailh Choc Navy", "Kailh Choc Jade", "Kailh Choc Pink", "Kailh Choc Robin",
            "Kailh Choc Pro Red", "Kailh Choc Silver", "Kailh Choc Sunset", "Kailh Choc Purpz",
            "Kailh Choc Red Pro", "Kailh Choc Crystal Red",
            "Kailh Choc V2 Red", "Kailh Choc V2 Brown", "Kailh Choc V2 Blue",
            "Kailh Choc V2 Silent Linear", "Kailh Choc V2 Silent Tactile",
            "Kailh Low Profile Red", "Kailh Low Profile Brown", "Kailh Low Profile Blue",
            "Kailh Low Profile White", "Kailh Low Profile Silver",
            "Kailh Prestige Red", "Kailh Prestige Light", "Kailh Prestige Silent", "Kailh Prestige Clicky",
            "Kailh Prestige Voice", "Kailh Prestige Silent Tactile", "Kailh Prestige Magnetic",
            "Kailh Magnetic Red", "Kailh Magnetic Silver", "Kailh Magnetic White", "Kailh Magnetic Jade",
            "Kailh Magnetic God", "Kailh Magnetic Ice Cream", "Kailh Magnetic Pink", "Kailh Sun Magnetic",
            "Kailh Box Glazed Green",
            "Kailh Optical Red", "Kailh Optical Black", "Kailh Optical Brown", "Kailh Optical Blue"
        ),
        slug("TTC") to options(
            "TTC Red", "TTC Brown", "TTC Blue", "TTC Black",
            "TTC Golden Red V3", "TTC Golden Brown V3", "TTC Golden Blue V3",
            "TTC Gold Pink", "TTC Gold Pink V2", "TTC Gold Silver",
            "TTC Speed Silver", "TTC Quick Silver", "TTC Ace", "TTC Bluish White", "TTC Bluish White Silent",
            "TTC Flame Red", "TTC Hey", "TTC Wild 42g", "TTC Wild 55g",
            "TTC Silent Red V3", "TTC Silent Brown V2", "TTC Frozen Silent", "TTC Frozen Silent V2",
            "TTC Silent Bluish White", "TTC Honey", "TTC Watermelon Milkshake",
            "TTC Tiger", "TTC Panda", "TTC Rabbit", "TTC Venus", "TTC Neptune", "TTC Matrix",
            "TTC Brother", "TTC Iron", "TTC Demon",
            "TTC RGB Red", "TTC RGB Brown", "TTC RGB Blue",
            "TTC Low Profile Red", "TTC Low Profile Brown", "TTC Low Profile Blue",
            "TTC Low Profile Speed Silver", "TTC Low Profile Silent Red",
            "TTC Magnetic King", "TTC Magnetic King of Magnetic", "TTC Magnetic RGB",
            "TTC Uranus Magnetic", "TTC KOM RGB Magnetic"
        ),
        slug("Outemu") to options(
            "Outemu Red", "Outemu Black", "Outemu Brown", "Outemu Blue", "Outemu Green",
            "Outemu Purple", "Outemu Silver", "Outemu Orange", "Outemu Clear",
            "Outemu Silent White", "Outemu Silent Grey", "Outemu Silent Peach", "Outemu Silent Peach V2",
            "Outemu Silent Lemon", "Outemu Silent Lemon V2", "Outemu Silent Cream Yellow",
            "Outemu Silent Cream Black", "Outemu Silent Ocean", "Outemu Silent Jade Yellow",
            "Outemu Silent Tom", "Outemu Silent Honey Peach",
            "Outemu Cream Pink", "Outemu Cream Blue", "Outemu Cream Yellow", "Outemu Cream Green",
            "Outemu Dustproof Red", "Outemu Dustproof Black", "Outemu Dustproof Brown",
            "Outemu Dustproof Blue", "Outemu Dustproof Purple",
            "Outemu Ice Purple", "Outemu Sky", "Outemu Phoenix", "Outemu Panda", "Outemu Lime",
            "Outemu Maple Leaf", "Outemu Milk Tea", "Outemu Dopamine", "Outemu Spring Breeze",
            "Outemu Cold Plum", "Outemu Lotus", "Outemu Crystal",
            "Outemu Low Profile Red", "Outemu Low Profile Brown", "Outemu Low Profile Blue",
            "Outemu Optical Red", "Outemu Optical Brown", "Outemu Optical Blue",
            "Outemu Magnetic Red", "Outemu Magnetic Jade", "Outemu Magnetic Silver"
        ),
        slug("Akko") to options(
            "Akko CS Rose Red", "Akko CS Matcha Green", "Akko CS Lavender Purple", "Akko CS Ocean Blue",
            "Akko CS Vintage White", "Akko CS Crystal", "Akko CS Jelly Black", "Akko CS Jelly White",
            "Akko CS Jelly Pink", "Akko CS Jelly Purple", "Akko CS Silver", "Akko CS Radiant Red",
            "Akko CS Sponge", "Akko CS Starfish", "Akko CS Wine Red", "Akko CS Air", "Akko CS Snow Blue Grey",
            "Akko V3 Cream Yellow", "Akko V3 Cream Blue", "Akko V3 Cream Black Pro", "Akko V3 Cream Yellow Pro",
            "Akko V3 Cream Blue Pro", "Akko V3 Lavender Purple Pro", "Akko V3 Piano Pro", "Akko V3 Silver Pro",
            "Akko V3 Crystal Pro", "Akko V3 Matcha Green Pro", "Akko V3 Wine Red Pro",
            "Akko V3 Pro Sakura", "Akko V3 Pro Penguin", "Akko V3 Pro Fairy", "Akko V3 Pro Piano",
            "Akko V3 Pro Creamy Purple", "Akko V3 Pro Blue", "Akko V3 Pro Yellow", "Akko V3 Pro Black",
            "Akko V3 Pro Silver", "Akko V3 Pro Matcha Green", "Akko V3 Pro Lavender Purple",
            "Akko V3 Pro Crystal", "Akko V3 Pro Cream Black", "Akko V3 Pro Cream Yellow",
            "Akko Rosewood", "Akko Creamy Cyan", "Akko Creamy Purple Pro", "Akko Stellar Rose",
            "Akko Dracula", "Akko Botany", "Akko Mirror", "Akko Cilantro", "Akko Astrolink",
            "Akko Haze Pink Silent", "Akko Penguin Silent", "Akko Fairy Silent", "Akko Piano",
            "Akko POM Silver", "Akko POM Pink", "Akko POM Brown", "Akko Crystal",
            "Akko Cream Yellow Magnetic", "Akko Astrolink Magnetic", "Akko Rosewood Magnetic",
            "Akko Glare Magnetic", "Akko Flash Magnetic", "Akko Magnetic Jade",
            "Akko Low Profile Silver", "Akko Low Profile Piano"
        ),
        slug("Haimu") to options(
            "Haimu Heartbeat Silent Linear", "Haimu Whisper Silent Tactile", "Haimu Sea Salt Lemon",
            "Haimu Raw", "Haimu Black Knight", "Haimu Geon Black", "Haimu Geon White",
            "Haimu Viola Tricolor", "Haimu Pastel Lemon", "Haimu Pastel Peach", "Haimu Pastel Mint",
            "Haimu Pastel Thistle", "Haimu Midnight", "Haimu Sakura", "Haimu Whisper",
            "Haimu Silent Red", "Haimu Silent Yellow", "Haimu Magnetic Heart", "Haimu Magnetic White",
            "Haimu Magnetic Black", "Haimu Low Profile Red", "Haimu Low Profile Brown"
        ),
        slug("JWK / Durock") to options(
            "JWK Black Linear", "JWK Red Linear", "JWK Yellow Linear", "JWK T1 Tactile",
            "JWK Ultimate Black", "JWK Alpaca V2", "JWK Mauve", "JWK Moss", "JWK HaluHalo",
            "JWK Quartz V2", "JWK Poseidon", "JWK Splash Brothers", "JWK Bluey",
            "Durock L1 Linear 55g", "Durock L2 Linear 62g", "Durock L3 Linear 65g",
            "Durock L4 Linear 67g", "Durock L5 Linear 78g", "Durock T1 Tactile", "Durock T1 Shrimp Silent",
            "Durock Koala 62g", "Durock Koala 67g", "Durock Dolphin Silent", "Durock Daybreak Silent",
            "Durock Black Lotus", "Durock Blue Lotus", "Durock Creamy Yellow", "Durock Mamba",
            "Durock POM Linear", "Durock Piano POM", "Durock Sunflower POM T1", "Durock Burgundy",
            "Durock White Lotus", "Durock Ice King Linear", "Durock Ice King Tactile",
            "Durock Silent Linear 62g", "Durock Silent Linear 67g"
        ),
        slug("Everglide") to options(
            "Everglide Aqua King 55g", "Everglide Aqua King 62g", "Everglide Aqua King 67g",
            "Everglide Amber Orange", "Everglide Bamboo Green", "Everglide Crystal Purple",
            "Everglide Dark Jade Black", "Everglide Jade Green", "Everglide Moyu Black",
            "Everglide Oreo", "Everglide Sakura Pink", "Everglide Tourmaline Blue",
            "Everglide Water King", "Everglide Sunset Yellow", "Everglide Lightning Silver",
            "Everglide EG Aqua King V3"
        ),
        slug("KTT") to options(
            "KTT Red Wine", "KTT Rose", "KTT Strawberry", "KTT Peach", "KTT Mint", "KTT Sea Salt Lemon",
            "KTT Kang White", "KTT Kang White V3", "KTT Hyacinth", "KTT Hyacinth V2", "KTT Grapefruit",
            "KTT Matcha", "KTT Purple Star", "KTT Baby Blue", "KTT Chalk", "KTT Cabbage Tofu",
            "KTT Mallo", "KTT Macaron Blue", "KTT Macaron Orange", "KTT Macaron Pink",
            "KTT Cream", "KTT Darling", "KTT Custard", "KTT Vanilla Ice Cream",
            "KTT Silent Red", "KTT Silent Brown", "KTT Low Profile Red", "KTT Low Profile Brown"
        ),
        slug("Tecsee") to options(
            "Tecsee Carrot", "Tecsee Purple Panda", "Tecsee Sapphire V2", "Tecsee Ruby V2",
            "Tecsee Diamond", "Tecsee Ice Candy", "Tecsee Ice Milk", "Tecsee Coral", "Tecsee Blue Sky",
            "Tecsee Jadeite", "Tecsee Oreo", "Tecsee Mango Ice", "Tecsee Lychee",
            "Tecsee Neapolitan Ice Cream", "Tecsee Strawberry Ice", "Tecsee Snow Globe", "Tecsee Raw",
            "Tecsee Medium", "Tecsee Metal Coated", "Tecsee Kingfisher", "Tecsee Lake Blue",
            "Tecsee Hamster", "Tecsee Ice Grape", "Tecsee Pudding Medium", "Tecsee Purple Panda PME"
        ),
        slug("Gazzew") to options(
            "Gazzew Boba U4 62g", "Gazzew Boba U4 68g", "Gazzew Boba U4T 62g", "Gazzew Boba U4T 68g",
            "Gazzew Boba LT 55g", "Gazzew Boba LT 65g",
            "Gazzew Bobagum Silent Linear 52g", "Gazzew Bobagum Silent Linear 62g", "Gazzew Bobagum Silent Linear 68g",
            "Gazzew U4Tx Half-Thock 65g", "Gazzew Linear Thock 55g", "Gazzew Linear Thock 65g",
            "Gazzew Phoenix Clicky", "Gazzew U4T RGB", "Gazzew U4 Silent RGB"
        ),
        slug("Zeal") to options(
            "Zeal Tealio V2 67g", "Zeal Healio V2 63.5g", "Zeal Roselio 67g", "Zeal Sakurio 62g",
            "Zeal Zealio V2 62g", "Zeal Zealio V2 65g", "Zeal Zealio V2 67g", "Zeal Zealio V2 78g",
            "Zeal Zilent V2 62g", "Zeal Zilent V2 65g", "Zeal Zilent V2 67g", "Zeal Zilent V2 78g",
            "Zeal Clickiez 40g", "Zeal Clickiez 75g", "Zeal Crystal", "Zeal Pearlio"
        ),
        slug("Glorious") to options(
            "Glorious Lynx", "Glorious Panda", "Glorious Fox", "Glorious Raptor", "Glorious Heist",
            "Glorious Panda Lubed", "Glorious Lynx Lubed"
        ),
        slug("NovelKeys") to options(
            "NovelKeys Cream", "NovelKeys Cream Plus", "NovelKeys Cream Clickie", "NovelKeys Box Cream",
            "NovelKeys Silk Yellow", "NovelKeys Silk Red", "NovelKeys Silk Black", "NovelKeys Silk Olivia",
            "NovelKeys Blueberry", "NovelKeys Sherbet", "NovelKeys Nolives", "NovelKeys Dream Cream",
            "NovelKeys Cream Arc"
        ),
        slug("Keychron") to options(
            "Keychron K Pro Red", "Keychron K Pro Brown", "Keychron K Pro Blue", "Keychron K Pro Banana",
            "Keychron K Pro Mint", "Keychron K Pro Silver",
            "Keychron Silent K Pro Red", "Keychron Silent K Pro Brown",
            "Keychron Super Red", "Keychron Super Brown", "Keychron Super Banana", "Keychron Super Mint",
            "Keychron Jupiter Red", "Keychron Jupiter Brown", "Keychron Jupiter Banana",
            "Keychron Low Profile Optical Red", "Keychron Low Profile Optical Brown",
            "Keychron Low Profile Optical Blue", "Keychron Low Profile Optical Banana",
            "Keychron Low Profile Optical Mint", "Keychron Low Profile Optical Orange",
            "Keychron Low Profile Optical White", "Keychron Low Profile Optical Black",
            "Keychron Low Profile Gateron Red", "Keychron Low Profile Gateron Brown",
            "Keychron Low Profile Gateron Blue", "Keychron Low Profile Gateron Banana",
            "Keychron Low Profile Gateron Silver"
        ),
        slug("Razer") to options(
            "Razer Green Mechanical Switch", "Razer Orange Mechanical Switch", "Razer Yellow Mechanical Switch",
            "Razer Purple Optical Switch", "Razer Red Linear Optical Switch",
            "Razer Clicky Low-Profile Optical Switch", "Razer Linear Low-Profile Optical Switch",
            "Razer Gen-2 Analog Optical Switch", "Razer Ultra-Low-Profile Clicky Switch"
        ),
        slug("Logitech") to options(
            "Logitech Romer-G Tactile", "Logitech Romer-G Linear",
            "Logitech GX Blue Clicky", "Logitech GX Brown Tactile", "Logitech GX Red Linear",
            "Logitech GL Clicky", "Logitech GL Tactile", "Logitech GL Linear",
            "Logitech GX Magnetic Analog"
        ),
        slug("SteelSeries") to options(
            "SteelSeries QX2 Red", "SteelSeries QX2 Blue",
            "SteelSeries OmniPoint", "SteelSeries OmniPoint 2.0", "SteelSeries OmniPoint 3.0"
        ),
        slug("Corsair") to options(
            "Corsair OPX Optical Linear", "Corsair OPX RGB Optical Linear",
            "Corsair MGX Magnetic", "Corsair MGX Hyperdrive Magnetic",
            "Corsair MLX Red", "Corsair MLX Plasma"
        ),
        slug("Wooting / Lekker") to options(
            "Wooting Lekker L60", "Wooting Lekker L45", "Wooting Lekker L60 V2", "Wooting Lekker L45 V2",
            "Wooting Lekker Tikken"
        ),
        slug("NuPhy") to options(
            "NuPhy Aloe", "NuPhy Cowberry", "NuPhy Wisteria", "NuPhy Moss", "NuPhy Daisy",
            "NuPhy Red 2.0", "NuPhy Brown 2.0", "NuPhy Blue 2.0", "NuPhy Night Breeze",
            "NuPhy Rose Glacier", "NuPhy Baby Kangaroo", "NuPhy Mint", "NuPhy Raspberry",
            "NuPhy Lemon", "NuPhy Blush Nano", "NuPhy Max Magnetic Jade"
        ),
        slug("Topre / Electro-Capacitive") to options(
            "Topre 30g", "Topre 35g", "Topre 45g", "Topre 55g", "Topre Variable Weight",
            "Topre Silent 30g", "Topre Silent 45g", "Topre Silent 55g",
            "NIZ Electro-Capacitive 35g", "NIZ Electro-Capacitive 45g", "NIZ Electro-Capacitive 55g",
            "NIZ Plum 35g", "NIZ Plum 45g", "NIZ Plum 55g",
            "Varmilo EC V2 Daisy", "Varmilo EC V2 Sakura", "Varmilo EC V2 Rose", "Varmilo EC V2 Violet",
            "Varmilo EC V2 Orange", "Varmilo EC V2 Ivy", "Varmilo EC V2 Moxa"
        ),
        slug("Alps / Matias") to options(
            "Alps SKCM Blue", "Alps SKCM White", "Alps SKCM Cream", "Alps SKCM Orange",
            "Alps SKCM Salmon", "Alps SKCM Brown", "Alps SKCL Green", "Alps SKCL Yellow",
            "Alps SKCL Cream", "Alps SKCL Lock", "Alps SKBM White", "Alps SKBL Black",
            "Matias Click", "Matias Quiet Click", "Matias Quiet Linear", "Matias Linear"
        ),
        slug("Other Magnetic Switches") to options(
            "Geon Raw HE 40g", "Geon Raw HE 50g", "Geon Raw HE 60g", "Geon Raptor HE", "Geon Venom HE",
            "Wuque Studio Dash HE", "Wuque Studio Jade HE", "Wuque Studio Aurora Clear",
            "T‑Mag Magnetic Ice", "T‑Mag Magnetic Frozen", "T‑Mag Magnetic Racing",
            "KPrepublic MMD Princess Magnetic", "KPrepublic MMD Cream Magnetic", "KPrepublic MMD Matcha Magnetic",
            "Outemu Purple Dawn Magnetic", "Everglide Sticky Rice Magnetic", "Everglide Rice White Magnetic",
            "Lichicx Raw Magnetic", "Kailh and Glorious Fox HE"
        )
    )

    /** The model list for a given [brandId], narrowed the same way [FilterField.optionsForState] narrows SoC Model to SoC Brand. */
    fun modelsFor(brandId: String): List<FilterOption> = modelsByBrand[brandId].orEmpty()

    val allModels: List<FilterOption> = modelsByBrand.values.flatten().distinctBy { it.id }
}
