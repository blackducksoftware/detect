using DemoApp.Core;
using FluentAssertions;
using Xunit;

namespace DemoApp.Core.Tests;

/// <summary>
/// Sample unit tests for OrderService.
/// These pull in xunit, Moq, FluentAssertions — all test-only deps
/// that should be excluded from a production BOM.
/// </summary>
public class OrderServiceTests
{
    [Fact]
    public void ProcessOrder_ReturnsValidJson()
    {
        var service = new OrderService();

        var result = service.ProcessOrder("CUST-001", 99.99m);

        result.Should().NotBeNullOrEmpty();
        result.Should().Contain("CUST-001");
        result.Should().Contain("99.99");
    }

    [Fact]
    public void ProcessOrder_ContainsProcessedStatus()
    {
        var service = new OrderService();

        var result = service.ProcessOrder("CUST-002", 150.00m);

        result.Should().Contain("Processed");
    }
}

