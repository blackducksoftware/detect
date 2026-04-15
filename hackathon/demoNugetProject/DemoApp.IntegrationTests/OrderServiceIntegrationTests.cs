using DemoApp.Core;
using Xunit;

namespace DemoApp.IntegrationTests;

/// <summary>
/// Sample integration tests.
/// These pull in Testcontainers, Microsoft.AspNetCore.Mvc.Testing —
/// heavy test-only deps that should be excluded from a production BOM.
/// </summary>
public class OrderServiceIntegrationTests
{
    [Fact]
    public void ProcessOrder_EndToEnd_ReturnsJson()
    {
        var service = new OrderService();

        var result = service.ProcessOrder("INT-001", 250.00m);

        Assert.NotNull(result);
        Assert.Contains("INT-001", result);
    }
}

