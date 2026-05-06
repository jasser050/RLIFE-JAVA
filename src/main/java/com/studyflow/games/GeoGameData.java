package com.studyflow.games;

import java.util.List;

public final class GeoGameData {
    private GeoGameData() {}

    public record FlagCountry(String flag, String country) {}
    public record CapitalEntry(String country, String flag, String capital) {}
    public record MapRegion(String country, String continent, double x, double y, double w, double h) {}

    public static final List<FlagCountry> FLAGS = List.of(
            new FlagCountry("Ã°Å¸â€¡Â«Ã°Å¸â€¡Â·", "France"), new FlagCountry("Ã°Å¸â€¡Â©Ã°Å¸â€¡Âª", "Germany"), new FlagCountry("Ã°Å¸â€¡Â¬Ã°Å¸â€¡Â§", "United Kingdom"),
            new FlagCountry("Ã°Å¸â€¡ÂºÃ°Å¸â€¡Â¸", "USA"), new FlagCountry("Ã°Å¸â€¡Â¯Ã°Å¸â€¡Âµ", "Japan"), new FlagCountry("Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â³", "China"),
            new FlagCountry("Ã°Å¸â€¡Â§Ã°Å¸â€¡Â·", "Brazil"), new FlagCountry("Ã°Å¸â€¡Â®Ã°Å¸â€¡Â³", "India"), new FlagCountry("Ã°Å¸â€¡Â®Ã°Å¸â€¡Â¹", "Italy"),
            new FlagCountry("Ã°Å¸â€¡ÂªÃ°Å¸â€¡Â¸", "Spain"), new FlagCountry("Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â¹", "Portugal"), new FlagCountry("Ã°Å¸â€¡Â³Ã°Å¸â€¡Â±", "Netherlands"),
            new FlagCountry("Ã°Å¸â€¡Â§Ã°Å¸â€¡Âª", "Belgium"), new FlagCountry("Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â­", "Switzerland"), new FlagCountry("Ã°Å¸â€¡Â¦Ã°Å¸â€¡Â¹", "Austria"),
            new FlagCountry("Ã°Å¸â€¡Â¸Ã°Å¸â€¡Âª", "Sweden"), new FlagCountry("Ã°Å¸â€¡Â³Ã°Å¸â€¡Â´", "Norway"), new FlagCountry("Ã°Å¸â€¡Â©Ã°Å¸â€¡Â°", "Denmark"),
            new FlagCountry("Ã°Å¸â€¡Â«Ã°Å¸â€¡Â®", "Finland"), new FlagCountry("Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â±", "Poland"), new FlagCountry("Ã°Å¸â€¡Â·Ã°Å¸â€¡Âº", "Russia"),
            new FlagCountry("Ã°Å¸â€¡ÂºÃ°Å¸â€¡Â¦", "Ukraine"), new FlagCountry("Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â·", "Turkey"), new FlagCountry("Ã°Å¸â€¡Â¸Ã°Å¸â€¡Â¦", "Saudi Arabia"),
            new FlagCountry("Ã°Å¸â€¡Â¦Ã°Å¸â€¡Âª", "UAE"), new FlagCountry("Ã°Å¸â€¡ÂªÃ°Å¸â€¡Â¬", "Egypt"), new FlagCountry("Ã°Å¸â€¡Â²Ã°Å¸â€¡Â¦", "Morocco"),
            new FlagCountry("Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â³", "Tunisia"), new FlagCountry("Ã°Å¸â€¡Â¿Ã°Å¸â€¡Â¦", "South Africa"), new FlagCountry("Ã°Å¸â€¡Â°Ã°Å¸â€¡Âª", "Kenya"),
            new FlagCountry("Ã°Å¸â€¡Â³Ã°Å¸â€¡Â¬", "Nigeria"), new FlagCountry("Ã°Å¸â€¡Â¬Ã°Å¸â€¡Â­", "Ghana"), new FlagCountry("Ã°Å¸â€¡Â¦Ã°Å¸â€¡Âº", "Australia"),
            new FlagCountry("Ã°Å¸â€¡Â³Ã°Å¸â€¡Â¿", "New Zealand"), new FlagCountry("Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â¦", "Canada"), new FlagCountry("Ã°Å¸â€¡Â²Ã°Å¸â€¡Â½", "Mexico"),
            new FlagCountry("Ã°Å¸â€¡Â¦Ã°Å¸â€¡Â·", "Argentina"), new FlagCountry("Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â±", "Chile"), new FlagCountry("Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â´", "Colombia"),
            new FlagCountry("Ã°Å¸â€¡ÂµÃ°Å¸â€¡Âª", "Peru"), new FlagCountry("Ã°Å¸â€¡Â°Ã°Å¸â€¡Â·", "South Korea"), new FlagCountry("Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â­", "Thailand"),
            new FlagCountry("Ã°Å¸â€¡Â»Ã°Å¸â€¡Â³", "Vietnam"), new FlagCountry("Ã°Å¸â€¡Â®Ã°Å¸â€¡Â©", "Indonesia"), new FlagCountry("Ã°Å¸â€¡Â²Ã°Å¸â€¡Â¾", "Malaysia"),
            new FlagCountry("Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â­", "Philippines"), new FlagCountry("Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â°", "Pakistan"), new FlagCountry("Ã°Å¸â€¡Â®Ã°Å¸â€¡Â·", "Iran"),
            new FlagCountry("Ã°Å¸â€¡Â®Ã°Å¸â€¡Â¶", "Iraq"), new FlagCountry("Ã°Å¸â€¡Â¯Ã°Å¸â€¡Â´", "Jordan")
    );

    public static final List<CapitalEntry> CAPITALS = List.of(
            new CapitalEntry("France", "Ã°Å¸â€¡Â«Ã°Å¸â€¡Â·", "Paris"), new CapitalEntry("Germany", "Ã°Å¸â€¡Â©Ã°Å¸â€¡Âª", "Berlin"),
            new CapitalEntry("United Kingdom", "Ã°Å¸â€¡Â¬Ã°Å¸â€¡Â§", "London"), new CapitalEntry("USA", "Ã°Å¸â€¡ÂºÃ°Å¸â€¡Â¸", "Washington D.C."),
            new CapitalEntry("Japan", "Ã°Å¸â€¡Â¯Ã°Å¸â€¡Âµ", "Tokyo"), new CapitalEntry("China", "Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â³", "Beijing"),
            new CapitalEntry("Brazil", "Ã°Å¸â€¡Â§Ã°Å¸â€¡Â·", "Brasilia"), new CapitalEntry("India", "Ã°Å¸â€¡Â®Ã°Å¸â€¡Â³", "New Delhi"),
            new CapitalEntry("Italy", "Ã°Å¸â€¡Â®Ã°Å¸â€¡Â¹", "Rome"), new CapitalEntry("Spain", "Ã°Å¸â€¡ÂªÃ°Å¸â€¡Â¸", "Madrid"),
            new CapitalEntry("Portugal", "Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â¹", "Lisbon"), new CapitalEntry("Australia", "Ã°Å¸â€¡Â¦Ã°Å¸â€¡Âº", "Canberra"),
            new CapitalEntry("Canada", "Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â¦", "Ottawa"), new CapitalEntry("Russia", "Ã°Å¸â€¡Â·Ã°Å¸â€¡Âº", "Moscow"),
            new CapitalEntry("Egypt", "Ã°Å¸â€¡ÂªÃ°Å¸â€¡Â¬", "Cairo"), new CapitalEntry("Morocco", "Ã°Å¸â€¡Â²Ã°Å¸â€¡Â¦", "Rabat"),
            new CapitalEntry("Tunisia", "Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â³", "Tunis"), new CapitalEntry("South Africa", "Ã°Å¸â€¡Â¿Ã°Å¸â€¡Â¦", "Pretoria"),
            new CapitalEntry("Kenya", "Ã°Å¸â€¡Â°Ã°Å¸â€¡Âª", "Nairobi"), new CapitalEntry("Saudi Arabia", "Ã°Å¸â€¡Â¸Ã°Å¸â€¡Â¦", "Riyadh"),
            new CapitalEntry("UAE", "Ã°Å¸â€¡Â¦Ã°Å¸â€¡Âª", "Abu Dhabi"), new CapitalEntry("Turkey", "Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â·", "Ankara"),
            new CapitalEntry("South Korea", "Ã°Å¸â€¡Â°Ã°Å¸â€¡Â·", "Seoul"), new CapitalEntry("Thailand", "Ã°Å¸â€¡Â¹Ã°Å¸â€¡Â­", "Bangkok"),
            new CapitalEntry("Argentina", "Ã°Å¸â€¡Â¦Ã°Å¸â€¡Â·", "Buenos Aires"), new CapitalEntry("Mexico", "Ã°Å¸â€¡Â²Ã°Å¸â€¡Â½", "Mexico City"),
            new CapitalEntry("Chile", "Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â±", "Santiago"), new CapitalEntry("Colombia", "Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â´", "Bogota"),
            new CapitalEntry("Peru", "Ã°Å¸â€¡ÂµÃ°Å¸â€¡Âª", "Lima"), new CapitalEntry("Pakistan", "Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â°", "Islamabad"),
            new CapitalEntry("Iran", "Ã°Å¸â€¡Â®Ã°Å¸â€¡Â·", "Tehran"), new CapitalEntry("Iraq", "Ã°Å¸â€¡Â®Ã°Å¸â€¡Â¶", "Baghdad"),
            new CapitalEntry("Jordan", "Ã°Å¸â€¡Â¯Ã°Å¸â€¡Â´", "Amman"), new CapitalEntry("Switzerland", "Ã°Å¸â€¡Â¨Ã°Å¸â€¡Â­", "Bern"),
            new CapitalEntry("Austria", "Ã°Å¸â€¡Â¦Ã°Å¸â€¡Â¹", "Vienna"), new CapitalEntry("Belgium", "Ã°Å¸â€¡Â§Ã°Å¸â€¡Âª", "Brussels"),
            new CapitalEntry("Netherlands", "Ã°Å¸â€¡Â³Ã°Å¸â€¡Â±", "Amsterdam"), new CapitalEntry("Sweden", "Ã°Å¸â€¡Â¸Ã°Å¸â€¡Âª", "Stockholm"),
            new CapitalEntry("Norway", "Ã°Å¸â€¡Â³Ã°Å¸â€¡Â´", "Oslo"), new CapitalEntry("Denmark", "Ã°Å¸â€¡Â©Ã°Å¸â€¡Â°", "Copenhagen"),
            new CapitalEntry("Finland", "Ã°Å¸â€¡Â«Ã°Å¸â€¡Â®", "Helsinki"), new CapitalEntry("Poland", "Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â±", "Warsaw"),
            new CapitalEntry("Ukraine", "Ã°Å¸â€¡ÂºÃ°Å¸â€¡Â¦", "Kyiv"), new CapitalEntry("Nigeria", "Ã°Å¸â€¡Â³Ã°Å¸â€¡Â¬", "Abuja"),
            new CapitalEntry("Ghana", "Ã°Å¸â€¡Â¬Ã°Å¸â€¡Â­", "Accra"), new CapitalEntry("New Zealand", "Ã°Å¸â€¡Â³Ã°Å¸â€¡Â¿", "Wellington"),
            new CapitalEntry("Vietnam", "Ã°Å¸â€¡Â»Ã°Å¸â€¡Â³", "Hanoi"), new CapitalEntry("Indonesia", "Ã°Å¸â€¡Â®Ã°Å¸â€¡Â©", "Jakarta"),
            new CapitalEntry("Malaysia", "Ã°Å¸â€¡Â²Ã°Å¸â€¡Â¾", "Kuala Lumpur"), new CapitalEntry("Philippines", "Ã°Å¸â€¡ÂµÃ°Å¸â€¡Â­", "Manila")
    );

    public static final List<MapRegion> MAP_REGIONS = List.of(
            new MapRegion("France", "Europe", 340, 170, 28, 20),
            new MapRegion("Germany", "Europe", 372, 162, 28, 20),
            new MapRegion("Spain", "Europe", 320, 198, 34, 20),
            new MapRegion("Italy", "Europe", 383, 194, 24, 24),
            new MapRegion("UK", "Europe", 312, 146, 22, 20),
            new MapRegion("USA", "Americas", 130, 170, 70, 35),
            new MapRegion("Canada", "Americas", 120, 132, 90, 30),
            new MapRegion("Mexico", "Americas", 172, 210, 40, 24),
            new MapRegion("Brazil", "Americas", 222, 286, 60, 50),
            new MapRegion("Argentina", "Americas", 236, 350, 35, 60),
            new MapRegion("Egypt", "Africa", 410, 248, 25, 20),
            new MapRegion("Morocco", "Africa", 360, 248, 25, 20),
            new MapRegion("Tunisia", "Africa", 392, 250, 16, 18),
            new MapRegion("South Africa", "Africa", 426, 375, 38, 24),
            new MapRegion("Saudi Arabia", "Asia", 456, 246, 42, 26),
            new MapRegion("India", "Asia", 530, 264, 36, 30),
            new MapRegion("China", "Asia", 570, 216, 70, 48),
            new MapRegion("Japan", "Asia", 658, 226, 20, 28),
            new MapRegion("South Korea", "Asia", 634, 226, 18, 20),
            new MapRegion("Australia", "Oceania", 642, 358, 74, 44)
    );
}

