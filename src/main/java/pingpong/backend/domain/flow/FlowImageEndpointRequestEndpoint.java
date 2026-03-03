package pingpong.backend.domain.flow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pingpong.backend.domain.swagger.Endpoint;

@Getter
@Entity
@Table(
	name = "flow_image_endpoint_request_endpoint",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_flow_image_endpoint_request_endpoint",
		columnNames = {"request_id", "endpoint_id"}
	)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FlowImageEndpointRequestEndpoint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "request_id", nullable = false)
	private FlowImageEndpointRequest request;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "endpoint_id", nullable = false)
	private Endpoint endpoint;

	@Column
	private Boolean isChanged;

	@Column
	private Boolean isLinked;

	public static FlowImageEndpointRequestEndpoint create(FlowImageEndpointRequest request, Endpoint endpoint) {
		FlowImageEndpointRequestEndpoint link = new FlowImageEndpointRequestEndpoint();
		link.request = request;
		link.endpoint = endpoint;
		link.isChanged = endpoint.getIsChanged();
		link.isLinked = false;
		return link;
	}

	public void markLinked() {
		this.isLinked = true;
	}

	public void markChanged() {
		this.isChanged = true;
	}
}
