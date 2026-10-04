package br.com.tempofamilia.data

/**
 * Lista padrão de termos e domínios adultos bloqueados.
 *
 * A comparação ignora acentos, maiúsculas, espaços e pontuação, e procura o termo
 * DENTRO do texto. Por isso evitamos palavras curtas que aparecem dentro de palavras
 * comuns (ex.: "anal" está em "análise", "puta" em "computação", "nua" em "continua").
 * Expressões compostas ("videos de sexo") são mais seguras e pegam variações.
 */
object DefaultTerms {

    val PORTUGUES = listOf(
        "porno", "pornografia", "pornografico", "pornozao", "pornodoido",
        "sexo explicito", "video de sexo", "videos de sexo", "filme de sexo", "filmes de sexo",
        "fotos de sexo", "foto de sexo", "sexo gratis", "sexo ao vivo", "sexo amador",
        "sexo anal", "sexo oral", "sexo grupal", "sexo virtual", "fazendo sexo",
        "cena de sexo", "cenas de sexo", "chat de sexo", "contos eroticos", "conto erotico",
        "videos eroticos", "video erotico", "filme erotico", "filmes eroticos",
        "transando", "transa gostosa", "putaria", "putinha", "safadinha", "novinha safada",
        "novinhas safadas", "mulheres safadas", "buceta", "bucetinha", "xoxota", "xereca",
        "boquete", "punheta", "siririca", "gozando na", "gozada na",
        "mulher pelada", "mulheres peladas", "peladona", "peladinha", "garotas peladas",
        "mulher nua", "mulheres nuas", "garotas nuas", "novinhas nuas", "fotos nuas",
        "nudes vazados", "nudes vazadas", "fotos intimas vazadas", "videos intimos vazados",
        "garota de programa", "garotas de programa", "brasileirinhas", "incesto",
        "pack do pezinho", "vazou nudes", "nudes",
    )

    val INGLES = listOf(
        "porn", "porno", "pornhub", "pornography", "xxx video", "xxx videos", "xxx porn",
        "xxx movie", "videos xxx", "filmes xxx", "fotos xxx", "xxx hd", "sex video", "sex videos",
        "free sex", "sex tape", "sex cam", "sex chat", "live sex", "hardcore sex", "sex movie",
        "camgirl", "camgirls", "camwhore", "nsfw", "hentai", "ecchi", "rule34", "rule 34",
        "milf", "blowjob", "handjob", "deepthroat", "gangbang", "creampie", "cumshot",
        "bukkake", "threesome sex", "masturbat", "fetish porn", "bdsm", "pussy", "boobs",
        "naked girls", "naked women", "naked girl", "nude girls", "nude pics", "nude photos",
        "striptease", "upskirt", "erotic video", "erotic videos", "lesbian sex", "gay sex",
        "anal sex", "teen sex", "onlyfans leak", "onlyfans leaks", "leaked nudes", "sexting",
        "playboy", "escort girls", "adult video", "adult videos", "adult content",
    )

    /** Domínios e marcas de sites adultos conhecidos. */
    val DOMINIOS = listOf(
        "xvideos", "xnxx", "xhamster", "redtube", "youporn", "youjizz", "spankbang",
        "spankwire", "brazzers", "onlyfans", "fansly", "chaturbate", "stripchat", "bongacams",
        "livejasmin", "cam4.com", "camsoda", "myfreecams", "eporner", "tnaflix", "tube8",
        "beeg.com", "motherless", "nhentai", "e-hentai", "hanime", "hentaihaven",
        "xxxbunker", "sunporno", "txxx", "hclips", "porntrex", "thumbzilla", "fapello",
        "erome.com", "sexlog", "redgifs", "imagefap", "literotica", "efukt", "heavy-r.com",
        "daftsex", "noodlemagazine", "sxyprn", "porndig", "drtuber", "nuvid", "ixxx.com",
        "hqporner", "4tube.com", "fuq.com", "manyvids", "clips4sale", "camwhores",
        "xvideos.red", "pornpics", "porn.com", "xxx.com", "sex.com", "rule34.xxx",
        "f95zone", "javhd", "javlibrary", "missav", "pornone", "porntube", "tubegalore",
        "vporn", "xozilla", "ashemaletube", "keezmovies", "extremetube", "mofosex",
        "pornhat", "anysex", "hdzog", "upornia", "voyeurhit", "privatehomeclips",
        "kwai porn", "camera prive", "cameraprive", "xvideos2", "xnxx2", "pornolandia",
        "sexoquente", "putariabrasileira", "kinkyfans", "privacy.com.br", "xxxvideos",
        "pornozinho", "videosporno", "xvideosbrasil", "xxnx",
    )

    val ALL: List<String> = (PORTUGUES + INGLES + DOMINIOS).distinct()
}
