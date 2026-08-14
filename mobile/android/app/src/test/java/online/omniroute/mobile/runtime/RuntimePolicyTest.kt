package online.omniroute.mobile.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RuntimePolicyTest {
    @Test
    fun `accepts child path inside managed root`() {
        val policy = RuntimePolicy()
        assertEquals("~/.omniroute-mobile/runtime/logs/runtime.log", policy.validateChildPath("~/.omniroute-mobile/runtime/logs/runtime.log"))
    }

    @Test
    fun `rejects path traversal outside managed root`() {
        val policy = RuntimePolicy()
        assertFailsWith<IllegalArgumentException> {
            policy.validateChildPath("~/.omniroute-mobile/runtime/../secrets")
        }
    }
}
