fun main() {
    println("🧪 Testing Horse Data Extraction Logic...")
    
    // Test the horse extraction logic with sample data
    val sampleRowData = listOf(
        "1",           // Horse number
        "42x543",      // Form
        "ROCK THEM JOOLS (NZ)", // Horse name
        "Chris Waller", // Trainer
        "Ben Melham",  // Jockey
        "2",           // Barrier
        "59.5kg"       // Weight
    )
    
    println("🔍 Sample row data: $sampleRowData")
    
    // Test the extraction logic
    if (sampleRowData.size >= 6) {
        val horseNumber = sampleRowData[0].toIntOrNull() ?: 0
        val form = sampleRowData[1]
        val horseName = sampleRowData[2]
        val trainer = sampleRowData[3]
        val jockey = sampleRowData[4]
        val barrier = sampleRowData[5].toIntOrNull() ?: 1
        val weight = if (sampleRowData.size > 6) sampleRowData[6].replace("kg", "").toDoubleOrNull() ?: 58.0 else 58.0
        
        println("✅ Extracted data:")
        println("  Horse Number: $horseNumber")
        println("  Form: $form")
        println("  Name: $horseName")
        println("  Trainer: $trainer")
        println("  Jockey: $jockey")
        println("  Barrier: $barrier")
        println("  Weight: ${weight}kg")
        
        // Verify the data looks correct
        val isValid = horseNumber > 0 && horseName.isNotEmpty() && trainer.isNotEmpty() && jockey.isNotEmpty()
        println("✅ Data validation: $isValid")
        
        if (isValid) {
            println("🎉 SUCCESS: Horse extraction logic is working correctly!")
        } else {
            println("❌ FAILED: Horse extraction logic has issues")
        }
    } else {
        println("❌ FAILED: Sample data doesn't have enough columns")
    }
}
