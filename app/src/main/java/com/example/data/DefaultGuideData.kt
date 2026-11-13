package com.example.data

object DefaultGuideData {

    val SUBJECT_ORDER = listOf(
        "বাংলা",
        "ইংরেজি",
        "আইসিটি",
        "পদার্থবিজ্ঞান",
        "রসায়ন",
        "জীববিজ্ঞান",
        "উচ্চতর গণিত"
    )

    fun create(): GuideDatabase {
        val subjects = linkedMapOf<String, SubjectData>()

        subjects["বাংলা"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১: পরিচিতি", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২: গদ্য", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৩: পদ্য", "https://drive.google.com/file/d/1m0r3OdI5UJ6xkK8VtHpP5zI_q6Mq6aOZ/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    ),
                    ResourceItem(
                        title = "বেসিক ধারণা (Royal)",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1m0r3OdI5UJ6xkK8VtHpP5zI_q6Mq6aOZ/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "নিউটন tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["ইংরেজি"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("Unit 1", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("Unit 2", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    ),
                    ResourceItem(
                        title = "বেসিক ধারণা (Royal)",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1m0r3OdI5UJ6xkK8VtHpP5zI_q6Mq6aOZ/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("Unit 1", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["আইসিটি"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["পদার্থবিজ্ঞান"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "রয়েল tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ১ রয়েল 2027", "https://drive.google.com/file/d/1v4dKhkEkxxo2MtZFDeLb4dRQLNps0Qqd/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২ রয়েল 2027", "https://drive.google.com/file/d/1B5qux7BKYt9SA392JFDNc_Ub4G0x0Sq4/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৩ রয়েল 2027", "https://drive.google.com/file/d/1P-0GpXlkhtr_N6AvtuGaEWUXHGByA91p/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৪ রয়েল 2027", "https://drive.google.com/file/d/1IOI1_bqL5DjRlw3jUd-dBh79DeIiSnOk/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৫ রয়েল 2027", "https://drive.google.com/file/d/1oZLMxzeSMEI6NR-JY1DXn4vY1QNseJdM/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৬ রয়েল 2027", "https://drive.google.com/file/d/1kdCuz03M1wRJcU4mHdga4FJv2ivDxoDg/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৭ রয়েল 2027", "https://drive.google.com/file/d/1yWcs6tqi_yMPpV-CHDHvmNIbBY9KrfuP/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৮ রয়েল 2027", "https://drive.google.com/file/d/17BQd7Fr8AontCUpyxwROQ4yaW1aUQ_hk/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ৯ রয়েল 2027", "https://drive.google.com/file/d/1C7rsr45dlDuT8lRjbVWJ48fjtCGpQK8L/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ১০ রয়েল 2027", "https://drive.google.com/file/d/1IjcNOibxPeZn9ydYYSLQ-JBPEKv7mIxT/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["রসায়ন"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("রয়েল ১ম পত্রিকার গাইড ২০২৬", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("(26)QPS_Che!1st part:01", "https://drive.google.com/file/d/17LenHnFRe7RzKIF6vhfGOKK_2u8yIr66/preview?usp=drivesdk"),
                            ChapterItem("(26)QPS_Che!2nd part:02", "https://drive.google.com/file/d/1yzYlA6RVxwRGDpl2Ob-acQwDPiVbicoM/preview?usp=drivesdk"),
                            ChapterItem("(26) রয়েল ১ম পত্র গাইড", "https://drive.google.com/file/d/1f7sJgAJZecQrbiMdDORgPqRF2JNXkT0c/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    ),
                    ResourceItem(
                        title = "বেসিক ধারণা (Royal)",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1m0r3OdI5UJ6xkK8VtHpP5zI_q6Mq6aOZ/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("রয়েল ১ম পত্রিকার গাইড ২০২৬", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("(26)QPS_Che!1st part:01", "https://drive.google.com/file/d/17LenHnFRe7RzKIF6vhfGOKK_2u8yIr66/preview?usp=drivesdk"),
                            ChapterItem("(26)QPS_Che!2nd part:02", "https://drive.google.com/file/d/1yzYlA6RVxwRGDpl2Ob-acQwDPiVbicoM/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("(26)QPS_Che!2nd part:01", "https://drive.google.com/file/d/1SYS-qGoPN0zZgQnwBm5I9G0Wt8WFbEEM/preview?usp=drivesdk"),
                            ChapterItem("(26)QPS_Che!2nd part:02", "https://drive.google.com/file/d/1SZEbdimZvKmdp3U0vtUlXmrLYjpyiWux/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["জীববিজ্ঞান"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("QPS(26) Biology 1st paper", "https://drive.google.com/file/d/12izZxebMZ5diIlEG7_9OdY4jLIBKjSGn/preview?usp=drivesdk"),
                            ChapterItem("QPS(26) 1st preview part:01", "https://drive.google.com/file/d/139gKZu4Krknj3vhBz5e4UEXTNPVl2ObH/preview?usp=drivesdk"),
                            ChapterItem("QPS(26) 1st preview part:02", "https://drive.google.com/file/d/1xb8HkfyvwJSCJy-GgSPkFv-WAaAU0UXy/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://viphasibul792.github.io/All_in_one_guide-book/"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("QPS(26) Biology 2nd paper", "https://drive.google.com/file/d/1Ci2UC2KVX_WClXrffD9YuRhoXTibNgy8/preview?usp=drivesdk"),
                            ChapterItem("QPS(26) Biology 2nd paper part:01", "https://drive.google.com/file/d/1Jv4_w1PIfVJDeo9-T0WLRLqYgPpWlF-M/preview?usp=drivesdk"),
                            ChapterItem("QPS(26) Biology 2nd paper part:02", "https://drive.google.com/file/d/1rvPeJGkW4QVvub4PEDpw4o_C9Q0VVlHQ/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        subjects["উচ্চতর গণিত"] = SubjectData(
            first = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    ),
                    ResourceItem(
                        title = "সকল অধ্যায় MCQ",
                        isCQ = false,
                        link = "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk"
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"),
                            ChapterItem("অধ্যায় ২", "https://drive.google.com/file/d/1yxWcINececpor75squeH-hxEKD8ioOBz/preview?usp=drivesdk")
                        )
                    )
                )
            ),
            second = PaperData(
                mainBook = mutableListOf(
                    ResourceItem(
                        title = "সকল প্রকাশনির মূল বই",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                ),
                testPaper = mutableListOf(
                    ResourceItem(
                        title = "All tast paper",
                        isCQ = true,
                        chapters = mutableListOf(
                            ChapterItem("অধ্যায় ১", "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk")
                        )
                    )
                )
            )
        )

        return GuideDatabase(subjects = subjects)
    }
}
