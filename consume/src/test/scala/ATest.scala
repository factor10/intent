import intent._


class ATest extends TestSuite with Stateless:
    "3 + 3":
        "should be 6" in expect(3 + 3).toEqual(6)
